const API = "http://localhost:8080";

// --- Elements ---
const queryInput = document.getElementById("query");
const searchBtn = document.getElementById("searchBtn");
const resultsDiv = document.getElementById("results");
const resultsMeta = document.getElementById("resultsMeta");
const typeFilter = document.getElementById("typeFilter");
const seasonFilter = document.getElementById("seasonFilter");
const yearFilter = document.getElementById("yearFilter");

const openLoginBtn = document.getElementById("openLoginBtn");
const openUploadBtn = document.getElementById("openUploadBtn");
const logoutBtn = document.getElementById("logoutBtn");
const loggedOutDiv = document.getElementById("loggedOut");
const loggedInDiv = document.getElementById("loggedIn");
const userName = document.getElementById("userName");
const userRole = document.getElementById("userRole");
const userInitial = document.getElementById("userInitial");

const loginEmail = document.getElementById("loginEmail");
const loginPassword = document.getElementById("loginPassword");
const loginBtn = document.getElementById("loginBtn");

const uploadFile = document.getElementById("uploadFile");
const uploadCourseId = document.getElementById("uploadCourseId");
const uploadType = document.getElementById("uploadType");
const uploadYear = document.getElementById("uploadYear");
const uploadSeason = document.getElementById("uploadSeason");
const uploadDescription = document.getElementById("uploadDescription");
const uploadBtn = document.getElementById("uploadBtn");

const toasts = document.getElementById("toasts");

// --- Session state, kept in localStorage so a refresh stays signed in ---
let authToken = localStorage.getItem("token");
let authRole = localStorage.getItem("role");
let authName = localStorage.getItem("name");

// --- Modals ---
function openModal(id) {
    document.getElementById(id).classList.add("open");
}

function closeModal(id) {
    document.getElementById(id).classList.remove("open");
}

document.querySelectorAll("[data-close]").forEach((el) => {
    el.addEventListener("click", () => closeModal(el.dataset.close));
});

// Clicking the dark backdrop (but not the dialog itself) closes the modal
document.querySelectorAll(".modal-backdrop").forEach((backdrop) => {
    backdrop.addEventListener("click", (e) => {
        if (e.target === backdrop) backdrop.classList.remove("open");
    });
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        document.querySelectorAll(".modal-backdrop.open")
            .forEach((m) => m.classList.remove("open"));
    }
});

// --- Toasts ---
function toast(message, kind = "info") {
    const el = document.createElement("div");
    el.className = `toast ${kind}`;
    el.textContent = message;
    toasts.appendChild(el);

    setTimeout(() => {
        el.classList.add("fade-out");
        setTimeout(() => el.remove(), 300);
    }, 3000);
}

// --- Authentication ---
async function login() {
    const email = loginEmail.value.trim();
    const password = loginPassword.value;

    if (!email || !password) {
        toast("Enter your email and password", "error");
        return;
    }

    try {
        const response = await fetch(`${API}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            toast("Invalid email or password", "error");
            return;
        }

        const data = await response.json();
        authToken = data.token;
        authName = data.name;
        authRole = data.role;

        localStorage.setItem("token", authToken);
        localStorage.setItem("name", authName);
        localStorage.setItem("role", authRole);

        loginPassword.value = "";
        closeModal("loginModal");
        updateAuthUI();
        toast(`Welcome, ${authName}`, "success");
    } catch (error) {
        toast("Could not reach the server", "error");
    }
}

function logout() {
    authToken = null;
    authName = null;
    authRole = null;
    localStorage.clear();
    updateAuthUI();
    toast("Logged out", "info");
}

// Swaps the top bar between signed-out and signed-in, and shows
// the upload button only to lecturers
function updateAuthUI() {
    const signedIn = Boolean(authToken);

    loggedOutDiv.style.display = signedIn ? "none" : "flex";
    loggedInDiv.style.display = signedIn ? "flex" : "none";
    openUploadBtn.style.display = (signedIn && authRole === "LECTURER") ? "block" : "none";

    if (signedIn) {
        userName.textContent = authName;
        userRole.textContent = authRole;
        userInitial.textContent = authName.charAt(0).toUpperCase();
    }

    updateAdminButton();
}

// --- Search ---
async function search() {
    const query = queryInput.value.trim();
    if (!query) return;

    resultsMeta.textContent = "";
    resultsDiv.innerHTML = "<p>Searching…</p>";

    let url = `${API}/files/search?query=${encodeURIComponent(query)}&limit=10`;
    if (typeFilter.value) url += `&type=${typeFilter.value}`;
    if (seasonFilter.value) url += `&season=${seasonFilter.value}`;
    if (yearFilter.value) url += `&year=${yearFilter.value}`;

    try {
        const response = await fetch(url);
        showResults(await response.json(), query);
    } catch (error) {
        resultsDiv.innerHTML = "";
        toast("Could not reach the server", "error");
    }
}

function showResults(files, query) {
    if (files.length === 0) {
        resultsMeta.textContent = "";
        resultsDiv.innerHTML = "<p>No matching files found.</p>";
        return;
    }

    resultsMeta.textContent =
        `${files.length} result${files.length === 1 ? "" : "s"} for “${query}”`;
    resultsDiv.innerHTML = "";

    for (const file of files) {
        const lecturer = `${file.uploadedBy.firstName} ${file.uploadedBy.lastName}`;
        const card = document.createElement("div");
        card.className = "card";
        card.innerHTML = `
            <div class="card-main">
                <h3>${file.course.name}<span class="badge">${file.type}</span></h3>
                <p class="meta">${lecturer} · ${file.year} ${file.season} · ${file.course.code}</p>
                <p class="filename">${file.originalName}</p>
            </div>
            <a href="${API}/files/${file.id}/download">Download</a>
        `;
        resultsDiv.appendChild(card);
    }
}

// --- Upload, lecturers only ---
async function upload() {
    if (!uploadFile.files[0]) {
        toast("Choose a file first", "error");
        return;
    }
    if (!uploadCourseId.value) {
        toast("Enter a course ID", "error");
        return;
    }

    // Files travel as multipart form data, not JSON
    const formData = new FormData();
    formData.append("file", uploadFile.files[0]);

    let url = `${API}/files/upload`
        + `?type=${uploadType.value}`
        + `&courseId=${uploadCourseId.value}`
        + `&year=${uploadYear.value}`
        + `&season=${uploadSeason.value}`;

    if (uploadDescription.value) {
        url += `&description=${encodeURIComponent(uploadDescription.value)}`;
    }

    toast("Uploading…", "info");

    try {
        // The token proves this user is a lecturer; the server reads
        // the uploader's identity from it
        const response = await fetch(url, {
            method: "POST",
            headers: { "Authorization": `Bearer ${authToken}` },
            body: formData
        });

        if (!response.ok) {
            toast(`Upload failed (${response.status})`, "error");
            return;
        }

        const file = await response.json();
        toast(`Uploaded: ${file.originalName}`, "success");

        uploadFile.value = "";
        uploadDescription.value = "";
        uploadCourseId.value = "";
        closeModal("uploadModal");
    } catch (error) {
        toast("Could not reach the server", "error");
    }
}

// --- Events ---
openLoginBtn.addEventListener("click", () => openModal("loginModal"));
openUploadBtn.addEventListener("click", () => openModal("uploadModal"));
loginBtn.addEventListener("click", login);
logoutBtn.addEventListener("click", logout);
uploadBtn.addEventListener("click", upload);
searchBtn.addEventListener("click", search);

queryInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") search();
});
loginPassword.addEventListener("keydown", (e) => {
    if (e.key === "Enter") login();
});

// ===== Admin panel =====
const openAdminBtn = document.getElementById("openAdminBtn");

// Show the Admin button only for admins (extend updateAuthUI)
function updateAdminButton() {
    openAdminBtn.style.display =
        (authToken && authRole === "ADMIN") ? "block" : "none";
}

// Tab switching
document.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
        document.querySelectorAll(".tab-pane").forEach((p) => p.classList.remove("active"));
        btn.classList.add("active");
        document.getElementById("tab-" + btn.dataset.tab).classList.add("active");
    });
});

// Helper: authenticated POST for admin actions
async function adminPost(url, body, isForm = false) {
    const options = {
        method: "POST",
        headers: { "Authorization": `Bearer ${authToken}` }
    };
    if (isForm) {
        options.body = body;
    } else {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(`Failed (${response.status})`);
    return response.json();
}

// Helper: GET a list
async function getList(path) {
    const response = await fetch(`${API}${path}`);
    return response.json();
}

// Fill a <select> with items -> {value:id, label:text}
function fillSelect(selectId, items, labelFn) {
    const select = document.getElementById(selectId);
    select.innerHTML = "";
    for (const item of items) {
        const opt = document.createElement("option");
        opt.value = item.id;
        opt.textContent = labelFn(item);
        select.appendChild(opt);
    }
}

// Fill an "existing items" list box
function fillList(listId, items, labelFn) {
    const box = document.getElementById(listId);
    if (!box) return;
    box.innerHTML = "";
    for (const item of items) {
        const row = document.createElement("div");
        row.className = "existing-item";
        row.innerHTML = `<span>${labelFn(item)}</span><span class="id">#${item.id}</span>`;
        box.appendChild(row);
    }
}

// Load everything the admin panel needs, and refresh all dropdowns/lists
async function refreshAdminData() {
    const [faculties, departments, courses, lecturers, students] = await Promise.all([
        getList("/faculties"),
        getList("/departments"),
        getList("/courses"),
        getList("/lecturers"),
        getList("/students")
    ]);

    fillSelect("depFaculty", faculties, (f) => f.name);
    fillSelect("crsDepartment", departments, (d) => d.name);
    fillSelect("lecDepartment", departments, (d) => d.name);
    fillSelect("stuDepartment", departments, (d) => d.name);
    fillSelect("aliasCourse", courses, (c) => `${c.name} (${c.code})`);

    fillList("listFaculties", faculties, (f) => f.name);
    fillList("listDepartments", departments, (d) => `${d.name} — ${d.code}`);
    fillList("listCourses", courses, (c) => `${c.name} (${c.code})`);
    fillList("listLecturers", lecturers, (l) => `${l.firstName} ${l.lastName}`);
    fillList("listStudents", students, (s) => `${s.firstName} ${s.lastName}`);
}

openAdminBtn.addEventListener("click", async () => {
    openModal("adminModal");
    try {
        await refreshAdminData();
    } catch (e) {
        toast("Could not load admin data", "error");
    }
});

// ---- Create actions ----
document.getElementById("createFacBtn").addEventListener("click", async () => {
    const name = document.getElementById("facName").value.trim();
    if (!name) return toast("Enter a faculty name", "error");
    try {
        await adminPost(`${API}/faculties`, { name });
        document.getElementById("facName").value = "";
        toast("Faculty created", "success");
        refreshAdminData();
    } catch (e) { toast(e.message, "error"); }
});

document.getElementById("createDepBtn").addEventListener("click", async () => {
    const facultyId = document.getElementById("depFaculty").value;
    const code = document.getElementById("depCode").value.trim();
    const name = document.getElementById("depName").value.trim();
    if (!facultyId || !code || !name) return toast("Fill all department fields", "error");
    try {
        await adminPost(`${API}/departments`, { code, name, facultyId: Number(facultyId) });
        document.getElementById("depCode").value = "";
        document.getElementById("depName").value = "";
        toast("Department created", "success");
        refreshAdminData();
    } catch (e) { toast(e.message, "error"); }
});

document.getElementById("createCrsBtn").addEventListener("click", async () => {
    const departmentId = document.getElementById("crsDepartment").value;
    const code = document.getElementById("crsCode").value.trim();
    const name = document.getElementById("crsName").value.trim();
    const description = document.getElementById("crsDesc").value.trim() || null;
    if (!departmentId || !code || !name) return toast("Fill all course fields", "error");
    try {
        await adminPost(`${API}/courses`, { code, name, departmentId: Number(departmentId), description });
        document.getElementById("crsCode").value = "";
        document.getElementById("crsName").value = "";
        document.getElementById("crsDesc").value = "";
        toast("Course created", "success");
        refreshAdminData();
    } catch (e) { toast(e.message, "error"); }
});

document.getElementById("createLecBtn").addEventListener("click", async () => {
    const departmentId = document.getElementById("lecDepartment").value;
    const body = {
        title: document.getElementById("lecTitle").value,
        firstName: document.getElementById("lecFirst").value.trim(),
        lastName: document.getElementById("lecLast").value.trim(),
        email: document.getElementById("lecEmail").value.trim(),
        passwordHash: document.getElementById("lecPassword").value,
        departmentId: Number(departmentId)
    };
    if (!body.firstName || !body.email || !body.passwordHash || !departmentId)
        return toast("Fill all lecturer fields", "error");
    try {
        await adminPost(`${API}/lecturers`, body);
        document.getElementById("lecFirst").value = "";
        document.getElementById("lecLast").value = "";
        document.getElementById("lecEmail").value = "";
        document.getElementById("lecPassword").value = "";
        toast("Lecturer created", "success");
        refreshAdminData();
    } catch (e) { toast(e.message, "error"); }
});

document.getElementById("createStuBtn").addEventListener("click", async () => {
    const departmentId = document.getElementById("stuDepartment").value;
    const body = {
        studentNumber: document.getElementById("stuNumber").value.trim(),
        firstName: document.getElementById("stuFirst").value.trim(),
        lastName: document.getElementById("stuLast").value.trim(),
        email: document.getElementById("stuEmail").value.trim(),
        passwordHash: document.getElementById("stuPassword").value,
        departmentId: Number(departmentId)
    };
    if (!body.firstName || !body.email || !body.passwordHash || !departmentId)
        return toast("Fill all student fields", "error");
    try {
        await adminPost(`${API}/students`, body);
        document.getElementById("stuNumber").value = "";
        document.getElementById("stuFirst").value = "";
        document.getElementById("stuLast").value = "";
        document.getElementById("stuEmail").value = "";
        document.getElementById("stuPassword").value = "";
        toast("Student created", "success");
        refreshAdminData();
    } catch (e) { toast(e.message, "error"); }
});

document.getElementById("createAliasBtn").addEventListener("click", async () => {
    const courseId = document.getElementById("aliasCourse").value;
    const alias = document.getElementById("aliasText").value.trim();
    if (!courseId || !alias) return toast("Select a course and enter an alias", "error");
    try {
        await adminPost(`${API}/courses/${courseId}/aliases?alias=${encodeURIComponent(alias)}`, {});
        document.getElementById("aliasText").value = "";
        toast("Alias added", "success");
    } catch (e) { toast(e.message, "error"); }
});

updateAuthUI();