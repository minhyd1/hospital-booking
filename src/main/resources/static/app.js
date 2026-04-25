/**
 * Hospital Booking System - Frontend Core
 * Using Bootstrap 5 & Vanilla JS
 */

(() => {
    const API_BASE = "";
    const STORAGE_KEY = "hb_session";

    // --- STATE MANAGEMENT ---
    const state = {
        token: null,
        user: null, // { id, email, fullName, role }
        loading: false,
        notice: null, // { type: 'success'|'danger'|'info', message }
        specialties: [],
        currentRoute: "/",
        routeParams: {},
    };

    // --- UTILITIES ---
    const h = (str) => String(str ?? "").replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m]));

    const formatDateTime = (dt) => {
        if (!dt) return "-";
        return new Date(dt).toLocaleString("vi-VN", {
            year: "numeric", month: "2-digit", day: "2-digit",
            hour: "2-digit", minute: "2-digit"
        });
    };

    const formatCurrency = (val) => {
        if (val === null || val === undefined) return "-";
        return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(val);
    };


    const renderTimeRemaining = (t) => {
        if (!t) return "";
        if (t === "Đã qua giờ hẹn") {
            return `<div class="small mt-1"><span class="badge bg-danger">Đã qua giờ hẹn</span></div>`;
        }
        return `<div class="text-info small">${t}</div>`;
    };
    const setNotice = (type, message) => {
        state.notice = message ? { type, message } : null;
        render();
    };

    const setLoading = (val) => {
        state.loading = val;
        render();
    };

    // --- SESSION ---
    const loadSession = () => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
      try {
        const { token, user } = JSON.parse(saved);
        state.token = token;
        state.user = user;
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
        }
    };

    const saveSession = (token, user) => {
        state.token = token;
        state.user = user;
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }));
    };

    const clearSession = () => {
        state.token = null;
        state.user = null;
        localStorage.removeItem(STORAGE_KEY);
        navigate("/login");
    };

    // --- API CALLER ---
    async function callApi(path, options = {}) {
        const url = API_BASE + path;
        const headers = { "Accept": "application/json", ...(options.headers || {}) };
        if (options.json) headers["Content-Type"] = "application/json";
        if (state.token && options.auth !== false) headers["Authorization"] = `Bearer ${state.token}`;

        try {
            const res = await fetch(url, {
                method: options.method || "GET",
                headers,
                body: options.json ? JSON.stringify(options.json) : options.body,
            });

      const contentType = res.headers.get("content-type") || "";
      const rawText = await res.text();
      let data = rawText;

      if (contentType.includes("application/json")) {
        try {
          data = rawText ? JSON.parse(rawText) : null;
        } catch {
          data = rawText;
        }
      }

      if (res.status === 401) {
        if (state.token) clearSession();
      }

      if (!res.ok) {
        if (typeof data === "string") throw new Error(data || `Lỗi ${res.status}`);
        throw new Error(data?.message || data?.error || `Lỗi ${res.status}`);
      }

      return data;
        } catch (err) {
            console.error(`API Error [${path}]:`, err);
            throw err;
        }
    }

    // --- ROUTING ---
  const buildHash = (path, params) => {
    const qs = params ? new URLSearchParams(params).toString() : "";
    return `#${path}${qs ? `?${qs}` : ""}`;
  };

  const navigate = (path, params) => {
    window.location.hash = buildHash(path, params);
  };

    const handleRoute = async () => {
    const hash = window.location.hash || "#/";
    const raw = hash.replace(/^#/, "") || "/";
    const [path, qs] = raw.split("?");
    state.currentRoute = path || "/";
    const params = {};
    if (qs) new URLSearchParams(qs).forEach((v, k) => (params[k] = v));
    state.routeParams = params;
        state.notice = null; // Clear notice on navigate
        render();
    };

    window.addEventListener("hashchange", handleRoute);

    // --- VIEWS ---

    const viewLayout = (content) => {
        const user = state.user;
        const isAdmin = user?.role === "ADMIN";
        const isDoctor = user?.role === "DOCTOR";

        const navLinks = [
            { path: "/", label: "Trang chủ", show: true },
            { path: "/book", label: "Đặt lịch", show: user?.role === "PATIENT" },
            { path: "/appointments", label: "Lịch hẹn", show: !!user },
            { path: "/doctor/schedule", label: "Lịch làm việc", show: isDoctor },
            { path: "/admin/dashboard", label: "Quản trị", show: isAdmin },
        ];

        return `
      <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm sticky-top">
        <div class="container">
          <a class="navbar-brand fw-bold" href="#/">🏥 HospitalBooking</a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMain">
            <span class="navbar-toggler-icon"></span>
          </button>
          <div class="collapse navbar-collapse" id="navMain">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
              ${navLinks.filter(l => l.show).map(l => `
                <li class="nav-item">
                  <a class="nav-link ${state.currentRoute === l.path ? "active fw-bold" : ""}" href="#${l.path}">${l.label}</a>
                </li>
              `).join("")}
            </ul>
            <div class="d-flex align-items-center">
              ${user ? `
                <div class="dropdown">
                  <button class="btn btn-light dropdown-toggle btn-sm shadow-sm" type="button" data-bs-toggle="dropdown">
                    👤 ${h(user.fullName)} <span class="badge bg-info text-dark ms-1">${h(user.role)}</span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end shadow">
                    <li><a class="dropdown-item" href="#/profile">Thông tin cá nhân</a></li>
                    ${isDoctor ? `<li><a class="dropdown-item" href="#/doctor/profile">Hồ sơ bác sĩ</a></li>` : ""}
                    <li><button class="dropdown-item" id="refreshTokenBtn">Làm mới token</button></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><button class="dropdown-item text-danger" id="logoutBtn">Đăng xuất</button></li>
                  </ul>
                </div>
              ` : `
                <a href="#/login" class="btn btn-outline-light btn-sm me-2">Đăng nhập</a>
                <a href="#/register" class="btn btn-light btn-sm">Đăng ký</a>
              `}
            </div>
          </div>
        </div>
      </nav>
      <main class="py-4">
        <div class="container">
          ${state.notice ? `
            <div class="alert alert-${state.notice.type} alert-dismissible fade show shadow-sm" role="alert">
              ${h(state.notice.message)}
              <button type="button" class="btn-close" id="closeNoticeBtn"></button>
            </div>
          ` : ""}
          ${content}
        </div>
      </main>
      <footer class="bg-light py-4 mt-auto border-top">
        <div class="container text-center">
          <p class="text-muted mb-0">&copy; 2026 Hospital Booking System. Thiết kế bởi Trae AI.</p>
        </div>
      </footer>
    `;
    };

    const viewHome = () => {
        return `
      <div class="row align-items-center g-5 py-5">
        <div class="col-lg-6">
          <h1 class="display-4 fw-bold lh-1 mb-3 text-primary">Chăm sóc sức khỏe toàn diện cho bạn</h1>
          <p class="lead text-muted">Đặt lịch khám bệnh trực tuyến nhanh chóng, dễ dàng với các bác sĩ chuyên khoa hàng đầu. Tiết kiệm thời gian, bảo mật thông tin.</p>
          <div class="d-grid gap-2 d-md-flex justify-content-md-start">
            <a href="#/book" class="btn btn-primary btn-lg px-4 me-md-2 shadow">Đặt lịch ngay</a>
            <a href="#/login" class="btn btn-outline-secondary btn-lg px-4">Tìm hiểu thêm</a>
          </div>
        </div>
        <div class="col-lg-6 text-center">
          <img src="https://img.freepik.com/free-vector/doctors-concept-illustration_114360-1515.jpg" class="img-fluid rounded-3" alt="Medical Illustration" width="500">
        </div>
      </div>
      <div class="row g-4 mt-4">
        <div class="col-md-4">
          <div class="card h-100 border-0 shadow-sm text-center p-3">
            <div class="card-body">
              <div class="display-5 text-primary mb-3">📅</div>
              <h5 class="card-title fw-bold">Đặt lịch 24/7</h5>
              <p class="card-text text-muted">Chủ động chọn thời gian khám phù hợp với lịch trình của bạn bất cứ lúc nào.</p>
            </div>
          </div>
        </div>
        <div class="col-md-4">
          <div class="card h-100 border-0 shadow-sm text-center p-3">
            <div class="card-body">
              <div class="display-5 text-primary mb-3">👨‍⚕️</div>
              <h5 class="card-title fw-bold">Bác sĩ uy tín</h5>
              <p class="card-text text-muted">Đội ngũ bác sĩ chuyên môn cao, giàu kinh nghiệm từ các bệnh viện lớn.</p>
            </div>
          </div>
        </div>
        <div class="col-md-4">
          <div class="card h-100 border-0 shadow-sm text-center p-3">
            <div class="card-body">
              <div class="display-5 text-primary mb-3">🔒</div>
              <h5 class="card-title fw-bold">Bảo mật tuyệt đối</h5>
              <p class="card-text text-muted">Thông tin cá nhân và lịch sử khám bệnh của bạn được cam kết bảo mật.</p>
            </div>
          </div>
        </div>
      </div>
    `;
    };

    const viewLogin = () => `
    <div class="row justify-content-center py-5">
      <div class="col-md-5">
        <div class="card shadow border-0 rounded-4">
          <div class="card-body p-5">
            <h2 class="fw-bold mb-4 text-center">Đăng nhập</h2>
            <form id="loginForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Email</label>
                <input type="email" name="email" class="form-control form-control-lg bg-light border-0" placeholder="name@example.com" required>
              </div>
              <div class="mb-4">
                <label class="form-label fw-semibold">Mật khẩu</label>
                <input type="password" name="password" class="form-control form-control-lg bg-light border-0" placeholder="••••••••" required>
              </div>
              <div class="d-grid mb-3">
                <button type="submit" class="btn btn-primary btn-lg shadow" ${state.loading ? "disabled" : ""}>
                  ${state.loading ? '<span class="spinner-border spinner-border-sm me-2"></span>' : ""}Đăng nhập
                </button>
              </div>
              <div class="text-center">
                <a href="#/forgot-password" class="text-decoration-none text-muted small">Quên mật khẩu?</a>
              </div>
              <hr class="my-4">
              <div class="text-center">
                <span class="text-muted">Chưa có tài khoản?</span>
                <a href="#/register" class="text-decoration-none fw-bold">Đăng ký ngay</a>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewRegister = () => `
    <div class="row justify-content-center py-5">
      <div class="col-md-6">
        <div class="card shadow border-0 rounded-4">
          <div class="card-body p-5">
            <h2 class="fw-bold mb-4 text-center">Đăng ký tài khoản</h2>
            <form id="registerForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Họ và tên</label>
                <input type="text" name="fullName" class="form-control bg-light border-0" placeholder="Nguyễn Văn A" required>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Email</label>
                <input type="email" name="email" class="form-control bg-light border-0" placeholder="name@example.com" required>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Số điện thoại</label>
                <input type="tel" name="phone" class="form-control bg-light border-0" placeholder="0123456789">
              </div>
              <div class="mb-4">
                <label class="form-label fw-semibold">Mật khẩu</label>
                <input type="password" name="password" class="form-control bg-light border-0" placeholder="Tối thiểu 6 ký tự" required minlength="6">
              </div>
              <div class="d-grid mb-3">
                <button type="submit" class="btn btn-primary btn-lg shadow" ${state.loading ? "disabled" : ""}>
                  ${state.loading ? '<span class="spinner-border spinner-border-sm me-2"></span>' : ""}Tạo tài khoản
                </button>
              </div>
              <hr class="my-4">
              <div class="text-center">
                <span class="text-muted">Đã có tài khoản?</span>
                <a href="#/login" class="text-decoration-none fw-bold">Đăng nhập</a>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewForgotPassword = () => `
    <div class="row justify-content-center py-5">
      <div class="col-md-5">
        <div class="card shadow border-0 rounded-4">
          <div class="card-body p-5">
            <h2 class="fw-bold mb-3 text-center">Quên mật khẩu</h2>
            <p class="text-muted text-center mb-4">Nhập email của bạn để nhận hướng dẫn đặt lại mật khẩu.</p>
            <form id="forgotForm">
              <div class="mb-4">
                <label class="form-label fw-semibold">Email</label>
                <input type="email" name="email" class="form-control form-control-lg bg-light border-0" placeholder="name@example.com" required>
              </div>
              <div class="d-grid mb-3">
                <button type="submit" class="btn btn-primary btn-lg shadow" ${state.loading ? "disabled" : ""}>
                  Gửi yêu cầu
                </button>
              </div>
              <div class="text-center">
                <a href="#/login" class="text-decoration-none text-muted">Quay lại đăng nhập</a>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewBook = (data) => {
        const { specialties, doctors, slots, selected } = data;
        return `
      <h2 class="fw-bold mb-4">📅 Đặt lịch khám bệnh</h2>
      <div class="row g-4">
        <div class="col-md-8">
          <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
            <div class="card-header bg-primary text-white py-3">
              <h5 class="mb-0">Thông tin đăng ký</h5>
            </div>
            <div class="card-body p-4">
              <form id="bookForm">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">1. Chọn Chuyên khoa</label>
                    <select id="bookSpec" class="form-select bg-light border-0">
                      <option value="">-- Chọn chuyên khoa --</option>
                      ${specialties.map(s => `<option value="${s.id}" ${s.id == selected.specId ? "selected" : ""}>${h(s.name)}</option>`).join("")}
                    </select>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold d-flex justify-content-between align-items-center">
                      <span>2. Chọn Bác sĩ</span>
                      ${selected.docId ? `<a class="small text-decoration-none" href="#/doctor-detail?id=${selected.docId}">Xem hồ sơ</a>` : ""}
                    </label>
                    <select id="bookDoc" class="form-select bg-light border-0" ${!selected.specId ? "disabled" : ""}>
                      <option value="">-- Chọn bác sĩ --</option>
                      ${(doctors || []).map(d => `<option value="${d.id}" ${d.id == selected.docId ? "selected" : ""}>Bác sĩ ${h(d.fullName)}</option>`).join("")}
                    </select>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">3. Chọn Ngày khám</label>
                    <input type="date" id="bookDate" class="form-control bg-light border-0" value="${selected.date || ""}" ${!selected.docId ? "disabled" : ""}>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">4. Chọn Khung giờ</label>
                    <select name="scheduleId" class="form-select bg-light border-0" ${!selected.date ? "disabled" : ""}>
                      <option value="">-- Chọn khung giờ --</option>
                      ${(slots || []).map(s => `<option value="${s.id}">${s.startTime} - ${s.endTime}</option>`).join("")}
                    </select>
                  </div>
                  <div class="col-12">
                    <label class="form-label fw-semibold">5. Triệu chứng & Ghi chú</label>
                    <textarea name="symptoms" class="form-control bg-light border-0" rows="3" placeholder="Mô tả ngắn gọn tình trạng sức khỏe của bạn..."></textarea>
                  </div>
                </div>
                <div class="mt-4">
                  <button type="submit" class="btn btn-primary btn-lg w-100 shadow" ${!state.token ? "disabled" : ""}>
                    ${state.token ? "Xác nhận đặt lịch" : "Vui lòng đăng nhập để đặt lịch"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
        <div class="col-md-4">
          <div class="card border-0 shadow-sm rounded-4">
            <div class="card-body p-4">
              <h5 class="fw-bold mb-3">💡 Lưu ý</h5>
              <ul class="text-muted small">
                <li class="mb-2">Vui lòng đến trước giờ khám 15 phút để làm thủ tục.</li>
                <li class="mb-2">Lịch hẹn có thể hủy trước 2 tiếng so với giờ khám.</li>
                <li>Mọi thắc mắc vui lòng liên hệ hotline: 1900 1234.</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    `;
    };

    const viewAppointments = (data) => {
        const { upcoming, history } = data;
        const user = state.user; // lấy user từ state để dùng trong renderTable
        const renderTable = (list, canCancel) => `
      <div class="table-responsive">
        <table class="table align-middle table-hover">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>Thời gian</th>
              <th>Đối tác</th>
              <th>Chuyên khoa</th>
              <th>Triệu chứng</th>
              <th>Trạng thái</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            ${list.length ? list.map(a => `
              <tr>
                <td><span class="text-muted small">#${a.appointmentId}</span></td>
                <td>
                  <div class="fw-bold">${formatDateTime(a.appointmentDateTime)}</div>
                  ${renderTimeRemaining(a.timeRemaining)}
                </td>
                <td>
                  <div class="fw-semibold">${h(a.partnerName)}</div>
                </td>
                <td><span class="badge bg-light text-dark border">${h(a.specialtyName)}</span></td>
                <td><div class="text-truncate small" style="max-width: 150px;" title="${h(a.symptoms)}">${h(a.symptoms || "-")}</div></td>
                <td><span class="badge ${a.status === 'PENDING' ? 'bg-warning' : a.status === 'COMPLETED' ? 'bg-success' : 'bg-danger'}">${a.status}</span></td>
                <td class="text-end">
                  ${a.meetingLink ? `<a href="${a.meetingLink}" target="_blank" class="btn btn-sm btn-info text-white me-1">Meeting</a>` : ""}
                  ${canCancel && a.status === 'PENDING' ? `<button class="btn btn-sm btn-outline-danger cancelApptBtn" data-id="${a.appointmentId}" data-role="${user.role}">Hủy</button>` : ""}
                </td>
              </tr>
            `).join("") : '<tr><td colspan="7" class="text-center py-4 text-muted">Không có lịch hẹn nào.</td></tr>'}
          </tbody>
        </table>
      </div>
    `;

        return `
      <h2 class="fw-bold mb-4">📋 Lịch hẹn của tôi</h2>
      <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-header bg-white border-bottom py-3">
          <h5 class="mb-0 fw-bold text-primary">Lịch hẹn sắp tới</h5>
        </div>
        <div class="card-body p-0">
          ${renderTable(upcoming, true)}
        </div>
      </div>
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-header bg-white border-bottom py-3">
          <h5 class="mb-0 fw-bold text-secondary">Lịch sử khám bệnh</h5>
        </div>
        <div class="card-body p-0">
          ${renderTable(history, false)}
        </div>
      </div>
    `;
    };

    // View riêng cho ADMIN: hiển thị toàn bộ lịch hẹn hệ thống
    // Cột: ID, Thời gian, Bệnh nhân, Bác sĩ, Chuyên khoa, Triệu chứng, Trạng thái, Hành động
    const viewAdminAppointments = (data) => {
        const { upcoming, history } = data;

        const statusBadge = (s) => {
            const map = { PENDING: 'bg-warning text-dark', CONFIRMED: 'bg-primary', COMPLETED: 'bg-success', CANCELLED: 'bg-danger' };
            return `<span class="badge ${map[s] || 'bg-secondary'}">${s}</span>`;
        };

        const renderAdminTable = (list, canAct) => `
      <div class="table-responsive">
        <table class="table align-middle table-hover">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>Thời gian</th>
              <th>Bệnh nhân</th>
              <th>Bác sĩ</th>
              <th>Chuyên khoa</th>
              <th>Triệu chứng</th>
              <th>Trạng thái</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            ${list.length ? list.map(a => `
              <tr>
                <td><span class="text-muted small">#${a.appointmentId}</span></td>
                <td>
                  <div class="fw-bold">${formatDateTime(a.appointmentDateTime)}</div>
                  ${renderTimeRemaining(a.timeRemaining)}
                </td>
                <td><div class="fw-semibold">${h(a.patientName || a.partnerName || "-")}</div></td>
                <td><div class="fw-semibold">${h(a.doctorName || "-")}</div></td>
                <td><span class="badge bg-light text-dark border">${h(a.specialtyName)}</span></td>
                <td><div class="text-truncate small" style="max-width:130px;" title="${h(a.symptoms)}">${h(a.symptoms || "-")}</div></td>
                <td>${statusBadge(a.status)}</td>
                <td class="text-end" style="white-space:nowrap;">
                  ${a.meetingLink ? `<a href="${a.meetingLink}" target="_blank" class="btn btn-sm btn-info text-white me-1">Meeting</a>` : ""}
                  ${canAct && (a.status === "PENDING" || a.status === "CONFIRMED") ? `
                    <button class="btn btn-sm btn-success me-1 confirmApptBtn" data-id="${a.appointmentId}">Duyệt</button>
                    <button class="btn btn-sm btn-outline-danger adminCancelBtn" data-id="${a.appointmentId}">Huỷ</button>
                  ` : ""}
                  ${canAct && a.status === "CONFIRMED" ? `
                    <button class="btn btn-sm btn-primary completeApptBtn" data-id="${a.appointmentId}">Hoàn thành</button>
                  ` : ""}
                </td>
              </tr>
            `).join("") : '<tr><td colspan="8" class="text-center py-4 text-muted">Không có lịch hẹn nào.</td></tr>'}
          </tbody>
        </table>
      </div>
    `;

        return `
      <h2 class="fw-bold mb-4">📋 Quản lý lịch hẹn</h2>
      <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-header bg-white border-bottom py-3">
          <h5 class="mb-0 fw-bold text-primary">Lịch hẹn đang hoạt động</h5>
        </div>
        <div class="card-body p-0">
          ${renderAdminTable(upcoming, true)}
        </div>
      </div>
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-header bg-white border-bottom py-3">
          <h5 class="mb-0 fw-bold text-secondary">Lịch sử khám bệnh</h5>
        </div>
        <div class="card-body p-0">
          ${renderAdminTable(history, false)}
        </div>
      </div>
    `;
    };

    const viewProfile = (me) => `
    <h2 class="fw-bold mb-4">👤 Thông tin cá nhân</h2>
    <div class="row g-4">
      <div class="col-md-7">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Hồ sơ của tôi</h5>
            <form id="profileForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Email</label>
                <input type="text" class="form-control bg-light border-0" value="${h(me.email)}" disabled>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Họ và tên</label>
                <input type="text" name="fullName" class="form-control" value="${h(me.fullName)}" required>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Số điện thoại</label>
                <input type="tel" name="phone" class="form-control" value="${h(me.phone)}">
              </div>
              <button type="submit" class="btn btn-primary px-4 shadow">Lưu thay đổi</button>
            </form>
          </div>
        </div>
      </div>
      <div class="col-md-5">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Đổi mật khẩu</h5>
            <form id="passwordForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Mật khẩu cũ</label>
                <input type="password" name="oldPassword" class="form-control" required>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Mật khẩu mới</label>
                <input type="password" name="newPassword" class="form-control" required minlength="6">
              </div>
              <button type="submit" class="btn btn-warning px-4 shadow">Cập nhật mật khẩu</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewDoctorProfile = (me) => `
    <h2 class="fw-bold mb-4">🩺 Hồ sơ bác sĩ</h2>
    <div class="row g-4">
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Cập nhật thông tin</h5>
            <form id="doctorProfileForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Email</label>
                <input type="text" class="form-control bg-light border-0" value="${h(me.email)}" disabled>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Họ và tên</label>
                <input type="text" name="fullName" class="form-control" value="${h(me.fullName)}" required>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Số điện thoại</label>
                <input type="tel" name="phone" class="form-control" value="${h(me.phone)}">
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Chuyên khoa</label>
                <input type="text" class="form-control bg-light border-0" value="${h(me.specialtyName || "-")}" disabled>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Phí khám</label>
                <input type="number" step="0.01" min="0" name="consultationFee" class="form-control" value="${me.consultationFee ?? 0}">
              </div>
              <div class="mb-4">
                <label class="form-label fw-semibold">Giới thiệu</label>
                <textarea name="bio" class="form-control" rows="4">${h(me.bio || "")}</textarea>
              </div>
              <button type="submit" class="btn btn-primary px-4 shadow">Lưu hồ sơ bác sĩ</button>
            </form>
          </div>
        </div>
      </div>
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-3">Gợi ý</h5>
            <ul class="text-muted small">
              <li class="mb-2">Cập nhật phí khám và giới thiệu để bệnh nhân dễ lựa chọn.</li>
              <li class="mb-2">Chuyên khoa hiện đang hiển thị theo dữ liệu hệ thống.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewDoctorDetail = (doctor) => `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold mb-0">👨‍⚕️ Thông tin bác sĩ</h2>
      <a href="#/" class="btn btn-link text-decoration-none">← Quay lại</a>
    </div>
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <div class="row g-4">
          <div class="col-md-8">
            <h4 class="fw-bold mb-1">${h(doctor.fullName)}</h4>
            <div class="text-muted mb-3">${h(doctor.specialtyName || "-")}</div>
            <div class="mb-3">
              <span class="badge bg-light text-dark border me-2">Phí khám</span>
              <span class="fw-semibold">${doctor.consultationFee !== undefined && doctor.consultationFee !== null ? formatCurrency(doctor.consultationFee) : "-"}</span>
            </div>
            <div>
              <h6 class="fw-bold">Giới thiệu</h6>
              <div class="text-muted">${h(doctor.bio || "Chưa có thông tin.")}</div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="p-3 bg-light rounded-4">
              <div class="small text-muted mb-2">Bạn muốn đặt lịch với bác sĩ này?</div>
              <a href="#/book" class="btn btn-primary w-100 shadow">Đi tới trang đặt lịch</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewAdminUserEdit = (u) => `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold mb-0">✏️ Cập nhật người dùng</h2>
      <a href="#/admin/users" class="btn btn-link text-decoration-none">← Quay lại</a>
    </div>
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <form id="adminUserEditForm" data-id="${u.id}">
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label fw-semibold">ID</label>
              <input type="text" class="form-control bg-light border-0" value="#${u.id}" disabled>
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Vai trò</label>
              <input type="text" class="form-control bg-light border-0" value="${h(u.role)}" disabled>
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Email</label>
              <input type="text" class="form-control bg-light border-0" value="${h(u.email)}" disabled>
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Số điện thoại</label>
              <input type="tel" name="phone" class="form-control" value="${h(u.phone)}">
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Họ và tên</label>
              <input type="text" name="fullName" class="form-control" value="${h(u.fullName)}" required>
            </div>
          </div>
          <div class="mt-4">
            <button type="submit" class="btn btn-primary px-4 shadow" ${state.loading ? "disabled" : ""}>Lưu</button>
          </div>
        </form>
      </div>
    </div>
  `;

    const viewAdminScheduleBatch = (doctors) => `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold mb-0">🗓️ Tạo lịch làm việc hàng loạt</h2>
      <a href="#/admin/dashboard" class="btn btn-link text-decoration-none">← Quay lại</a>
    </div>
    <div class="row g-4">
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Thiết lập</h5>
            <form id="adminBatchScheduleForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Bác sĩ</label>
                <select name="doctorId" class="form-select" required>
                  <option value="">-- Chọn bác sĩ --</option>
                  ${(doctors || []).map(d => `<option value="${d.id}">#${d.id} · ${h(d.fullName)}${d.specialtyName ? ` · ${h(d.specialtyName)}` : ""}</option>`).join("")}
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label fw-semibold">Ngày làm việc</label>
                <input type="date" name="workingDate" class="form-control" required>
              </div>
              <div class="row g-2 mb-3">
                <div class="col-6">
                  <label class="form-label fw-semibold small">Bắt đầu</label>
                  <input type="time" name="startTime" class="form-control" required>
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold small">Kết thúc</label>
                  <input type="time" name="endTime" class="form-control" required>
                </div>
              </div>
              <div class="mb-4">
                <label class="form-label fw-semibold">Độ dài mỗi ca (phút)</label>
                <input type="number" name="slotMinutes" class="form-control" min="5" step="5" value="30" required>
              </div>
              <button type="submit" class="btn btn-primary w-100 shadow" ${state.loading ? "disabled" : ""}>Tạo hàng loạt</button>
            </form>
          </div>
        </div>
      </div>
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-3">Lưu ý</h5>
            <ul class="text-muted small mb-0">
              <li class="mb-2">Tính năng này gọi API batch để tạo nhiều ca theo khoảng thời gian.</li>
              <li class="mb-2">Nếu đã có ca trùng giờ trong DB, hệ thống backend hiện chưa kiểm tra trùng.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewAdminDashboard = (stats) => {
        const s = stats || {};
        const cards = [
            { label: "Tổng lịch hẹn", val: s.totalAppointments, icon: "📅", color: "primary" },
            { label: "Người dùng", val: s.totalUsers, icon: "👥", color: "info" },
            { label: "Chuyên khoa", val: s.totalSpecialties, icon: "🏥", color: "success" },
            { label: "Doanh thu", val: formatCurrency(s.totalRevenue), icon: "💰", color: "warning" },
        ];

        return `
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold mb-0">📊 Bảng quản trị</h2>
        <div class="btn-group shadow-sm">
          <a href="#/admin/users" class="btn btn-outline-primary btn-sm">Quản lý User</a>
          <a href="#/admin/specialties" class="btn btn-outline-primary btn-sm">Chuyên khoa</a>
          <a href="#/admin/schedule-batch" class="btn btn-outline-primary btn-sm">Lịch hàng loạt</a>
        </div>
      </div>
      <div class="row g-4 mb-5">
        ${cards.map(c => `
          <div class="col-md-3">
            <div class="card border-0 shadow-sm rounded-4 h-100 bg-${c.color} text-white">
              <div class="card-body p-4">
                <div class="d-flex justify-content-between align-items-center">
                  <div>
                    <div class="small opacity-75">${c.label}</div>
                    <div class="h3 fw-bold mb-0">${c.val || 0}</div>
                  </div>
                  <div class="display-6 opacity-50">${c.icon}</div>
                </div>
              </div>
            </div>
          </div>
        `).join("")}
      </div>
      <div class="card border-0 shadow-sm rounded-4">
        <div class="card-header bg-white py-3">
          <h5 class="mb-0 fw-bold">Thống kê trạng thái lịch hẹn</h5>
        </div>
        <div class="card-body">
          <div class="row text-center">
            <div class="col-4 border-end">
              <div class="h2 fw-bold text-success">${s.completedAppointments || 0}</div>
              <div class="small text-muted">Hoàn thành</div>
            </div>
            <div class="col-4 border-end">
              <div class="h2 fw-bold text-warning">${s.pendingAppointments || 0}</div>
              <div class="small text-muted">Đang chờ</div>
            </div>
            <div class="col-4">
              <div class="h2 fw-bold text-danger">${s.cancelledAppointments || 0}</div>
              <div class="small text-muted">Đã hủy</div>
            </div>
          </div>
        </div>
      </div>
    `;
    };

    const viewAdminUsers = (data) => {
        const { users, page, totalPages, roleFilter } = data;
        return `
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold mb-0">👥 Quản lý người dùng</h2>
        <a href="#/admin/dashboard" class="btn btn-link text-decoration-none">← Quay lại</a>
      </div>
      
      <div class="row g-4">
        <div class="col-lg-4">
          <div class="card border-0 shadow-sm rounded-4 mb-4">
            <div class="card-body p-4">
              <h5 class="fw-bold mb-4">➕ Thêm tài khoản mới</h5>
              <form id="adminCreateUserForm">
                <div class="mb-3">
                  <label class="form-label fw-semibold">Họ và tên</label>
                  <input type="text" name="fullName" class="form-control bg-light border-0" placeholder="Nguyễn Văn A" required>
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Email</label>
                  <input type="email" name="email" class="form-control bg-light border-0" placeholder="email@example.com" required>
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Số điện thoại</label>
                  <input type="tel" name="phone" class="form-control bg-light border-0" placeholder="0123456789">
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Vai trò</label>
                  <select name="role" class="form-select bg-light border-0" required>
                    <option value="PATIENT">Bệnh nhân</option>
                    <option value="DOCTOR">Bác sĩ</option>
                    <option value="ADMIN">Quản trị viên</option>
                  </select>
                </div>
                <div class="mb-4">
                  <label class="form-label fw-semibold">Mật khẩu mặc định</label>
                  <input type="password" name="password" class="form-control bg-light border-0" placeholder="••••••••" required minlength="6">
                </div>
                <button type="submit" class="btn btn-primary w-100 shadow" ${state.loading ? "disabled" : ""}>
                  Tạo tài khoản
                </button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-lg-8">
          <div class="card border-0 shadow-sm rounded-4 mb-4">
            <div class="card-body p-4">
              <div class="row g-3 mb-4">
                <div class="col-md-6">
                  <select id="userRoleFilter" class="form-select border-0 bg-light">
                    <option value="">Tất cả vai trò</option>
                    <option value="PATIENT" ${roleFilter === "PATIENT" ? "selected" : ""}>Bệnh nhân</option>
                    <option value="DOCTOR" ${roleFilter === "DOCTOR" ? "selected" : ""}>Bác sĩ</option>
                    <option value="ADMIN" ${roleFilter === "ADMIN" ? "selected" : ""}>Quản trị viên</option>
                  </select>
                </div>
              </div>
              <div class="table-responsive">
                <table class="table align-middle table-hover">
                  <thead class="table-light">
                    <tr>
                      <th>ID</th>
                      <th>Họ tên</th>
                      <th>Vai trò</th>
                      <th class="text-end">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    ${(users || []).map(u => `
                      <tr>
                        <td><span class="text-muted small">#${u.id}</span></td>
                        <td>
                          <div class="fw-semibold">${h(u.fullName)}</div>
                          <div class="text-muted small">${h(u.email)}</div>
                        </td>
                        <td><span class="badge bg-light text-dark border">${u.role}</span></td>
                        <td class="text-end">
                          <a class="btn btn-sm btn-outline-primary me-2" href="#/admin/user-edit?id=${u.id}">Sửa</a>
                          <button class="btn btn-sm btn-outline-danger deleteUserBtn" data-id="${u.id}">Xóa</button>
                        </td>
                      </tr>
                    `).join("")}
                    ${(!users || users.length === 0) ? '<tr><td colspan="4" class="text-center py-4 text-muted">Không có dữ liệu.</td></tr>' : ''}
                  </tbody>
                </table>
              </div>
              ${totalPages > 1 ? `
                <nav class="mt-4">
                  <ul class="pagination justify-content-center">
                    ${Array.from({ length: totalPages }, (_, i) => `
                      <li class="page-item ${i === page ? "active" : ""}">
                        <a class="page-link userPageLink" href="#" data-page="${i}">${i + 1}</a>
                      </li>
                    `).join("")}
                  </ul>
                </nav>
              ` : ""}
            </div>
          </div>
        </div>
      </div>
    `;
    };

    const viewAdminSpecialties = (list) => `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold mb-0">🏥 Quản lý chuyên khoa</h2>
      <a href="#/admin/dashboard" class="btn btn-link text-decoration-none">← Quay lại</a>
    </div>
    <div class="row g-4">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Thêm chuyên khoa</h5>
            <form id="specForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Tên chuyên khoa</label>
                <input type="text" name="name" class="form-control" required>
              </div>
              <div class="mb-4">
                <label class="form-label fw-semibold">Mô tả</label>
                <textarea name="description" class="form-control" rows="3"></textarea>
              </div>
              <button type="submit" class="btn btn-primary w-100 shadow">Thêm mới</button>
            </form>
          </div>
        </div>
      </div>
      <div class="col-md-8">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table align-middle table-hover mb-0">
                <thead class="table-light">
                  <tr>
                    <th>ID</th>
                    <th>Tên</th>
                    <th>Mô tả</th>
                    <th class="text-end"></th>
                  </tr>
                </thead>
                <tbody>
                  ${list.map(s => `
                    <tr>
                      <td><span class="text-muted small">#${s.id}</span></td>
                      <td class="fw-semibold">${h(s.name)}</td>
                      <td><div class="text-truncate small" style="max-width: 250px;">${h(s.description || "-")}</div></td>
                      <td class="text-end px-4">
                        <button class="btn btn-sm btn-outline-danger deleteSpecBtn" data-id="${s.id}">Xóa</button>
                      </td>
                    </tr>
                  `).join("")}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

    const viewDoctorSchedule = (slots, selectedDate) => `
    <h2 class="fw-bold mb-4">👨‍⚕️ Quản lý lịch làm việc</h2>
    <div class="row g-4">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h5 class="fw-bold mb-4">Thêm ca làm việc</h5>
            <form id="scheduleForm">
              <div class="mb-3">
                <label class="form-label fw-semibold">Ngày làm việc</label>
                <input type="date" name="workingDate" class="form-control" value="${selectedDate || ""}" required>
              </div>
              <div class="row g-2 mb-4">
                <div class="col-6">
                  <label class="form-label fw-semibold small">Bắt đầu</label>
                  <input type="time" name="startTime" class="form-control" required>
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold small">Kết thúc</label>
                  <input type="time" name="endTime" class="form-control" required>
                </div>
              </div>
              <button type="submit" class="btn btn-primary w-100 shadow">Tạo lịch</button>
            </form>
          </div>
        </div>
      </div>
      <div class="col-md-8">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-header bg-white py-3 border-bottom">
            <div class="row align-items-center">
              <div class="col">
                <h5 class="mb-0 fw-bold">Lịch làm việc trong ngày</h5>
              </div>
              <div class="col-auto">
                <input type="date" id="scheduleDateFilter" class="form-control form-control-sm border-0 bg-light" value="${selectedDate || ""}">
              </div>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table align-middle table-hover mb-0">
                <thead class="table-light">
                  <tr>
                    <th>Ngày</th>
                    <th>Thời gian</th>
                    <th>Trạng thái</th>
                    <th class="text-end"> Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  ${slots.length ? slots.map(s => `
                    <tr>
                      <td>${s.workingDate}</td>
                      <td class="fw-bold text-primary">${s.startTime} - ${s.endTime}</td>
                      <td>
                        <span class="badge ${s.isBooked ? "bg-danger" : "bg-success"}">
                          ${s.isBooked ? "Đã đặt" : "Trống"}
                        </span>
                      </td>
                      <td class="text-end px-4">
                        <button class="btn btn-sm btn-outline-danger deleteSlotBtn" data-id="${s.id}" ${s.isBooked ? "disabled" : ""}>Xóa</button>
                      </td>
                    </tr>
                  `).join("") : '<tr><td colspan="4" class="text-center py-5 text-muted">Không có lịch làm việc.</td></tr>'}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

    // --- RENDERING LOGIC ---

    const render = async () => {
        const app = document.getElementById("app");
        let content = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';

        // Preliminary data fetching for specific routes
        const r = state.currentRoute;
        try {
            if (state.token && !state.user?.id) {
                state.user = await callApi("/api/users/me");
            }

            if (r === "/") {
                content = viewHome();
            } else if (r === "/login") {
                content = viewLogin();
            } else if (r === "/register") {
                content = viewRegister();
            } else if (r === "/forgot-password") {
                content = viewForgotPassword();
            } else if (r === "/book") {
                if (!state.user) return navigate("/login");
                if (state.user.role !== "PATIENT") return navigate("/");
                if (!state.specialties.length) state.specialties = await callApi("/api/specialties", { auth: false });
                const specId = state.routeParams.specId || "";
                const docId = state.routeParams.docId || "";
                const date = state.routeParams.date || "";

                let doctors = [];
                if (specId) doctors = await callApi(`/api/users/doctors?specialtyId=${specId}`, { auth: false });

                let slots = [];
                if (docId && date) slots = await callApi(`/api/schedule/available?doctorId=${docId}&date=${date}`);

                content = viewBook({ specialties: state.specialties, doctors, slots, selected: { specId, docId, date } });
            } else if (r === "/appointments") {
                if (!state.user) return navigate("/login");
                const role = state.user.role;
                const id = state.user.id;
                let upcoming = [], history = [];
                if (role === "ADMIN") {
                    // ADMIN xem tất cả lịch hẹn qua GET /api/appointments
                    const all = await callApi("/api/appointments");
                    upcoming = all.filter(a => a.status === "PENDING" || a.status === "CONFIRMED");
                    history  = all.filter(a => a.status === "COMPLETED" || a.status === "CANCELLED");
                    content = viewAdminAppointments({ upcoming, history });
                } else if (role === "DOCTOR") {
                    upcoming = await callApi(`/api/appointments/upcoming/doctor/${id}`);
                    history  = await callApi(`/api/appointments/history/doctor/${id}`);
                    content = viewAppointments({ upcoming, history });
                } else {
                    upcoming = await callApi(`/api/appointments/upcoming/patient/${id}`);
                    history  = await callApi(`/api/appointments/history/patient/${id}`);
                    content = viewAppointments({ upcoming, history });
                }
            } else if (r === "/profile") {
                if (!state.user) return navigate("/login");
                const me = await callApi("/api/users/me");
                content = viewProfile(me);
            } else if (r === "/doctor/profile") {
                if (!state.user) return navigate("/login");
                if (state.user.role !== "DOCTOR") return navigate("/");
                const me = await callApi("/api/users/me");
                content = viewDoctorProfile(me);
            } else if (r === "/admin/dashboard") {
                if (state.user?.role !== "ADMIN") return navigate("/");
                const stats = await callApi("/api/statistics");
                content = viewAdminDashboard(stats);
            } else if (r === "/admin/users") {
                if (state.user?.role !== "ADMIN") return navigate("/");
                const role = state.routeParams.role || "";
                const page = parseInt(state.routeParams.page || "0");
                const data = await callApi(`/api/users?page=${page}&size=10${role ? `&role=${role}` : ""}`);
                content = viewAdminUsers({ users: data.data, page: data.currentPage, totalPages: data.totalPages, roleFilter: role });
            } else if (r === "/admin/user-edit") {
                if (state.user?.role !== "ADMIN") return navigate("/");
                const id = state.routeParams.id;
                if (!id) return navigate("/admin/users");
                const u = await callApi(`/api/users/${id}`);
                content = viewAdminUserEdit(u);
            } else if (r === "/admin/specialties") {
                if (state.user?.role !== "ADMIN") return navigate("/");
                const list = await callApi("/api/specialties", { auth: false });
                content = viewAdminSpecialties(list);
            } else if (r === "/admin/schedule-batch") {
                if (state.user?.role !== "ADMIN") return navigate("/");
                const data = await callApi("/api/users?role=DOCTOR&page=0&size=100");
                content = viewAdminScheduleBatch(data.data || []);
            } else if (r === "/doctor/schedule") {
                if (state.user?.role !== "DOCTOR") return navigate("/");
                const date = state.routeParams.date || new Date().toISOString().split('T')[0];
                const slots = await callApi(`/api/schedule/doctor?date=${date}`);
                content = viewDoctorSchedule(slots, date);
            } else if (r === "/doctor-detail") {
                const id = state.routeParams.id;
                if (!id) return navigate("/");
                const doctor = await callApi(`/api/users/doctors/${id}`, { auth: false });
                content = viewDoctorDetail(doctor);
            } else {
                content = '<div class="text-center py-5"><h3>404 - Trang không tồn tại</h3><a href="#/">Quay lại trang chủ</a></div>';
            }
        } catch (err) {
            content = `<div class="alert alert-danger shadow-sm"><strong>Lỗi:</strong> ${h(err.message)}</div><div class="text-center"><button class="btn btn-primary" onclick="window.location.reload()">Tải lại trang</button></div>`;
        }

        app.innerHTML = viewLayout(content);
        attachEvents();
    };

    const attachEvents = () => {
        // Global Close Notice
        document.getElementById("closeNoticeBtn")?.addEventListener("click", () => setNotice(null, null));

        // Logout
        document.getElementById("logoutBtn")?.addEventListener("click", clearSession);

        // Login Form
        document.getElementById("loginForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            setLoading(true);
            try {
                const res = await callApi("/api/users/login", {
                    method: "POST",
                    json: Object.fromEntries(fd),
                    auth: false
                });
                const role = res.role?.name || res.role;
                saveSession(res.token, { id: res.id, email: res.email, fullName: res.fullName, role });
                setNotice("success", "Chào mừng quay trở lại!");
                navigate("/");
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Register Form
        document.getElementById("registerForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            setLoading(true);
            try {
                await callApi("/api/users/register", { method: "POST", json: Object.fromEntries(fd), auth: false });
                setNotice("success", "Đăng ký thành công! Vui lòng đăng nhập.");
                navigate("/login");
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Forgot Password
        document.getElementById("forgotForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = e.target.email.value;
            setLoading(true);
            try {
                await callApi("/api/users/forgot-password", { method: "POST", json: { email }, auth: false });
                setNotice("success", "Yêu cầu đã được gửi. Vui lòng kiểm tra email.");
                navigate("/login");
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Booking Selects
        document.getElementById("bookSpec")?.addEventListener("change", (e) => {
            state.routeParams = { specId: e.target.value };
            render();
        });
        document.getElementById("bookDoc")?.addEventListener("change", (e) => {
            state.routeParams.docId = e.target.value;
            render();
        });
        document.getElementById("bookDate")?.addEventListener("change", (e) => {
            state.routeParams.date = e.target.value;
            render();
        });

        // Booking Submit
        document.getElementById("bookForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const data = Object.fromEntries(fd);
            if (!data.scheduleId) return setNotice("danger", "Vui lòng chọn khung giờ khám.");
            setLoading(true);
            try {
                await callApi("/api/appointments/book", { method: "POST", json: { scheduleId: parseInt(data.scheduleId), symptoms: data.symptoms } });
                setNotice("success", "Đặt lịch thành công!");
                navigate("/appointments");
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Cancel Appointment
        // PATIENT  → DELETE /api/appointments/{id}
        // DOCTOR   → PATCH  /api/appointments/{id}/status?status=CANCELLED
        document.querySelectorAll(".cancelApptBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Bạn có chắc chắn muốn hủy lịch hẹn này?")) return;
                try {
                    const isDoctor = btn.dataset.role === "DOCTOR";
                    if (isDoctor) {
                        await callApi(`/api/appointments/${btn.dataset.id}/status?status=CANCELLED`, { method: "PATCH" });
                    } else {
                        await callApi(`/api/appointments/${btn.dataset.id}`, { method: "DELETE" });
                    }
                    setNotice("success", "Đã hủy lịch hẹn.");
                    render();
                } catch (err) {
                    setNotice("danger", err.message);
                }
            });
        });

        // ADMIN: Duyệt lịch hẹn PENDING → CONFIRMED
        document.querySelectorAll(".confirmApptBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Duyệt lịch hẹn này?")) return;
                try {
                    await callApi(`/api/appointments/${btn.dataset.id}/status?status=CONFIRMED`, { method: "PATCH" });
                    setNotice("success", "Đã duyệt lịch hẹn!");
                    render();
                } catch (err) { setNotice("danger", err.message); }
            });
        });

        // ADMIN: Đánh dấu hoàn thành → COMPLETED
        document.querySelectorAll(".completeApptBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Đánh dấu lịch hẹn này là hoàn thành?")) return;
                try {
                    await callApi(`/api/appointments/${btn.dataset.id}/status?status=COMPLETED`, { method: "PATCH" });
                    setNotice("success", "Đã cập nhật trạng thái hoàn thành!");
                    render();
                } catch (err) { setNotice("danger", err.message); }
            });
        });

        // ADMIN: Huỷ lịch hẹn → CANCELLED
        document.querySelectorAll(".adminCancelBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Huỷ lịch hẹn này?")) return;
                try {
                    await callApi(`/api/appointments/${btn.dataset.id}/status?status=CANCELLED`, { method: "PATCH" });
                    setNotice("success", "Đã huỷ lịch hẹn!");
                    render();
                } catch (err) { setNotice("danger", err.message); }
            });
        });

        // Profile & Password
        document.getElementById("profileForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fullName = e.target.fullName.value;
            const phone = e.target.phone.value;
            try {
                await callApi("/api/users/profile", { method: "PUT", json: { fullName, phone } });
                state.user.fullName = fullName;
                saveSession(state.token, state.user);
                setNotice("success", "Cập nhật hồ sơ thành công.");
            } catch (err) {
                setNotice("danger", err.message);
            }
        });

        document.getElementById("passwordForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                await callApi("/api/users/change-password", { method: "POST", json: Object.fromEntries(fd) });
                setNotice("success", "Đổi mật khẩu thành công.");
                e.target.reset();
            } catch (err) {
                setNotice("danger", err.message);
            }
        });

        // Admin - Specialties
        document.getElementById("specForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                await callApi("/api/specialties", { method: "POST", json: Object.fromEntries(fd) });
                setNotice("success", "Thêm chuyên khoa thành công.");
                render();
            } catch (err) {
                setNotice("danger", err.message);
            }
        });

        document.querySelectorAll(".deleteSpecBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Xóa chuyên khoa này?")) return;
                try {
                    await callApi(`/api/specialties/${btn.dataset.id}`, { method: "DELETE" });
                    setNotice("success", "Đã xóa.");
                    render();
                } catch (err) {
                    setNotice("danger", err.message);
                }
            });
        });

        // Admin - Users
        document.getElementById("userRoleFilter")?.addEventListener("change", (e) => {
            state.routeParams = { role: e.target.value, page: 0 };
            render();
        });

        document.querySelectorAll(".userPageLink").forEach(link => {
            link.addEventListener("click", (e) => {
                e.preventDefault();
                state.routeParams.page = e.target.dataset.page;
                render();
            });
        });

        document.querySelectorAll(".deleteUserBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Xóa người dùng này?")) return;
                try {
                    await callApi(`/api/users/${btn.dataset.id}`, { method: "DELETE" });
                    setNotice("success", "Đã xóa.");
                    render();
                } catch (err) {
                    setNotice("danger", err.message);
                }
            });
        });

        // Admin - Create User Submit
        document.getElementById("adminCreateUserForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            setLoading(true);
            try {
                await callApi("/api/users/register", { method: "POST", json: Object.fromEntries(fd), auth: false });
                setNotice("success", "Tạo tài khoản thành công!");
                e.target.reset();
                render();
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Admin - Batch Schedule
        document.getElementById("adminBatchScheduleForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const doctorId = parseInt(fd.get("doctorId"), 10);
            const workingDate = fd.get("workingDate");
            const startTime = fd.get("startTime");
            const endTime = fd.get("endTime");
            const slotMinutes = parseInt(fd.get("slotMinutes"), 10);

            if (!doctorId || !workingDate) return setNotice("danger", "Vui lòng chọn bác sĩ và ngày làm việc.");
            if (!startTime || !endTime) return setNotice("danger", "Vui lòng chọn giờ bắt đầu/kết thúc.");
            if (!slotMinutes || slotMinutes <= 0) return setNotice("danger", "Độ dài mỗi ca không hợp lệ.");
            if (startTime >= endTime) return setNotice("danger", "Giờ kết thúc phải lớn hơn giờ bắt đầu.");

            const toMinutes = (t) => {
                const [hh, mm] = String(t).split(":").map(Number);
                return hh * 60 + mm;
            };
            const toTime = (mins) => {
                const hh = String(Math.floor(mins / 60)).padStart(2, "0");
                const mm = String(mins % 60).padStart(2, "0");
                return `${hh}:${mm}`;
            };

            const startM = toMinutes(startTime);
            const endM = toMinutes(endTime);
            const dtos = [];
            for (let m = startM; m + slotMinutes <= endM; m += slotMinutes) {
                dtos.push({
                    doctorId,
                    workingDate,
                    startTime: toTime(m),
                    endTime: toTime(m + slotMinutes),
                });
            }

            if (!dtos.length) return setNotice("danger", "Không tạo được ca nào với khoảng thời gian đã chọn.");

            setLoading(true);
            try {
                const res = await callApi("/api/schedule/admin/batch", { method: "POST", json: dtos });
                const created = typeof res === "object" && res ? res.created : dtos.length;
                const msg = typeof res === "object" && res ? res.message : res;
                setNotice("success", `${msg} (Đã tạo ${created} ca)`);
            } catch (err) {
                setNotice("danger", err.message);
            } finally {
                setLoading(false);
            }
        });

        // Doctor - Schedule
        document.getElementById("scheduleDateFilter")?.addEventListener("change", (e) => {
            state.routeParams.date = e.target.value;
            render();
        });

        document.getElementById("scheduleForm")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const json = Object.fromEntries(fd);
            json.doctorId = state.user.id;
            if (!json.workingDate) return setNotice("danger", "Vui lòng chọn ngày làm việc.");
            if (!json.startTime || !json.endTime) return setNotice("danger", "Vui lòng chọn giờ bắt đầu/kết thúc.");
            if (json.startTime >= json.endTime) return setNotice("danger", "Giờ kết thúc phải lớn hơn giờ bắt đầu.");
            try {
                const res = await callApi("/api/schedule", { method: "POST", json });
                state.routeParams.date = json.workingDate;
                setNotice("success", res?.message || "Đã tạo lịch làm việc.");
                render();
            } catch (err) {
                setNotice("danger", err.message);
            }
        });

        document.querySelectorAll(".deleteSlotBtn").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Xóa ca làm việc này?")) return;
                try {
                    await callApi(`/api/schedule/${btn.dataset.id}`, { method: "DELETE" });
                    setNotice("success", "Đã xóa.");
                    render();
                } catch (err) {
                    setNotice("danger", err.message);
                }
            });
        });
    };

    // --- INITIALIZATION ---
    loadSession();
    handleRoute();

})();
