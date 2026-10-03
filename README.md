# 📧 Mail Server JavaFX — UDP Socket

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.5-blue?style=flat-square)
![UDP](https://img.shields.io/badge/Protocol-UDP-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![Status](https://img.shields.io/badge/Status-Completed-success?style=flat-square)

> **Hệ thống Mail Server nội bộ** chạy trên giao thức **UDP Socket** — Giao diện desktop **JavaFX** hiện đại — Cho phép nhiều người dùng đăng ký tài khoản, đăng nhập, gửi và nhận thư trong cùng một mạng LAN.
>
> 📚 Bài tập Lab môn **Lập trình mạng** — Ứng dụng mô hình **Client-Server** với UDP.

---

## 📖 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Demo](#-demo)
- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc Project](#-cấu-trúc-project)
- [Cài đặt & Chạy](#-cài-đặt--chạy)
- [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)
- [Giao thức UDP](#-giao-thức-udp)
- [Test 2 máy LAN](#-test-2-máy-lan)
- [Screenshots](#-screenshots)
- [Tác giả](#-tác-giả)
- [License](#-license)

---

## 🎯 Giới thiệu

Đây là ứng dụng **Mail Server** (máy chủ thư) hoạt động theo mô hình **Client-Server**:

- **Server**: Lắng nghe trên cổng UDP `2023`, xử lý mọi yêu cầu từ client, quản lý tài khoản và thư.
- **Client**: Kết nối tới server qua UDP, cho phép người dùng đăng ký, đăng nhập, gửi/nhận thư.
- **Nhiều client** có thể kết nối đồng thời tới cùng một server qua mạng LAN.

Ứng dụng mô phỏng một hệ thống email nội bộ như Gmail nhưng **không cần internet** — chỉ hoạt động trong mạng LAN của bạn.

---

## 🎬 Demo

| Màn hình | Mô tả |
|----------|-------|
| **Đăng nhập** | Giao diện tối, có 2 tab Đăng nhập / Đăng ký |
| **Hộp thư đến** | Danh sách mail bên trái, nội dung bên phải |
| **Soạn thư** | Modal popup giữa màn hình (Telegram style) |
| **Server Dashboard** | 5 tab: Dashboard · Clients Online · Accounts · Mailbox · Live Log |
| **Biểu đồ realtime** | Line chart + Pie chart cập nhật mỗi giây |

---

## ✨ Tính năng

### 👤 Quản lý tài khoản
- ✅ **Đăng ký** tài khoản mới (username + password)
- ✅ **Đăng nhập** với xác thực user/password
- ✅ Mỗi user có email dạng `username@gmail.com`
- ✅ Tự động tạo thư mục trên server khi đăng ký
- ✅ File `new_email.txt` chứa thư chào mừng

### 📧 Quản lý thư
- ✅ **Gửi thư** đến người dùng khác
- ✅ **Xem danh sách** mail trong hộp thư
- ✅ **Đọc nội dung** mail chi tiết
- ✅ **Trả lời** mail (Reply)
- ✅ **Đánh dấu sao** (Star) mail quan trọng
- ✅ **Xóa** mail (chuyển vào Thùng rác)
- ✅ Xem mail **đã gửi** (Sent folder)
- ✅ **Auto-refresh** mỗi 3 giây

### 🌐 Kết nối mạng
- ✅ Giao thức **UDP Socket** (port 2023)
- ✅ **Kết nối LAN** nhiều máy cùng lúc
- ✅ Nhập **IP Server** linh hoạt (không cần localhost)
- ✅ **Timeout 5 giây** khi mất kết nối
- ✅ **Toast notification** khi có mail mới

### 🖥️ Server Dashboard
- ✅ **Biểu đồ line chart** realtime (requests/second)
- ✅ **Pie chart** phân phối lệnh (REGISTER/LOGIN/SEND)
- ✅ Bảng **Clients Online** — ai đang online/offline
- ✅ Bảng **Accounts** — danh sách tài khoản
- ✅ Bảng **Mailbox** — tất cả email trên server
- ✅ **Live Log** — mọi request hiển thị realtime

### 🎨 Giao diện
- ✅ **Dark theme** chuyên nghiệp (navy + cyan)
- ✅ **Icon FontAwesome** vector đẹp mắt
- ✅ **Focus glow** cyan khi click vào ô nhập
- ✅ **Animation** mượt mà khi chuyển tab
- ✅ **Hover effect** trên mọi nút

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| **Ngôn ngữ** | Java 21 (OpenJDK Temurin) |
| **Giao diện** | JavaFX 21.0.5 |
| **Icon** | Ikonli FontAwesome 5 (12.3.1) |
| **Giao thức** | UDP Socket (`DatagramSocket`) |
| **Lưu trữ** | File system (thư mục + file `.txt`) |
| **Build** | `javac` thủ công (không dùng Maven/Gradle) |
| **IDE khuyến nghị** | VS Code + Extension Pack for Java |

---

## 📁 Cấu trúc Project

```
MailServerJavaFX/
│
├── src/                              # Source code
│   ├── server/
│   │   ├── ServerCore.java           # Xử lý UDP socket, logic server
│   │   └── ServerApp.java            # Giao diện Server (dashboard)
│   │
│   └── client/
│       ├── ClientCore.java           # Gửi/nhận request UDP
│       └── ClientApp.java            # Giao diện Client (mail box)
│
├── lib/                              # Thư viện cần thiết
│   ├── ikonli-core-12.3.1.jar
│   ├── ikonli-javafx-12.3.1.jar
│   ├── ikonli-fontawesome5-pack-12.3.1.jar
│   └── *.css                         # Theme CSS (tuỳ chọn)
│
├── bin/                              # File .class sau khi compile (tự tạo)
├── run-server.bat                    # Chạy Server nhanh (Windows)
├── run-client.bat                    # Chạy Client nhanh (Windows)
├── .gitignore
├── README.md
└── LICENSE
```

### 📂 Cấu trúc dữ liệu trên Server

Khi chạy, server tự tạo thư mục `F:\server_mail\` (hoặc theo config) với cấu trúc:

```
F:\server_mail\
│
├── an\                               # Tài khoản user "an"
│   ├── user.txt                      # "an"
│   ├── pass.txt                      # Mật khẩu (plain text — demo)
│   ├── date.txt                      # Ngày đăng ký
│   ├── email.txt                     # "an@gmail.com"
│   ├── new_email.txt                 # Thư chào mừng
│   ├── starred.txt                   # Danh sách mail đã đánh dấu
│   │
│   ├── inbox\                        # Thư nhận
│   │   ├── welcome.txt
│   │   └── email_binh_20261004_153000.txt
│   │
│   ├── sent\                         # Thư đã gửi
│   │   └── email_an_20261004_152500.txt
│   │
│   └── trash\                        # Thùng rác
│       └── (mail đã xóa)
│
└── binh\                             # Tài khoản user "binh"
    └── ...
```

---

## 🚀 Cài đặt & Chạy

### Yêu cầu hệ thống

- **JDK 21** trở lên — [Tải Adoptium Temurin](https://adoptium.net/)
- **JavaFX SDK 21.0.5** — [Tải tại Gluon](https://gluonhq.com/products/javafx/)
- **Windows / Linux / macOS**

### Các bước cài đặt

#### 1. Clone project

```bash
git clone https://github.com/Chamm286/MailServerJavaFX.git
cd MailServerJavaFX
```

#### 2. Tải JavaFX SDK

- Vào https://gluonhq.com/products/javafx/
- Chọn: **JavaFX 21.0.5** · **Windows** · **x64** · **SDK**
- Tải file `.zip` → giải nén vào `lib/javafx-sdk-21.0.5/`

Cấu trúc sau khi giải nén:
```
lib/javafx-sdk-21.0.5/
├── bin/
└── lib/
    ├── javafx.base.jar
    ├── javafx.controls.jar
    ├── javafx.fxml.jar
    ├── javafx.graphics.jar
    └── ...
```

#### 3. Compile

Mở **CMD** tại thư mục project:

```cmd
cd /d F:\MailServerJavaFX

"C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot\bin\javac" ^
  -encoding UTF-8 -d bin ^
  --module-path "lib\javafx-sdk-21.0.5\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "lib\ikonli-core-12.3.1.jar;lib\ikonli-javafx-12.3.1.jar;lib\ikonli-fontawesome5-pack-12.3.1.jar" ^
  src\server\ServerCore.java src\server\ServerApp.java ^
  src\client\ClientCore.java src\client\ClientApp.java
```

> 💡 Nếu máy bạn có JDK ở chỗ khác → sửa đường dẫn `C:\Program Files\...` cho đúng.

#### 4. Chạy Server

**Terminal 1:**
```cmd
run-server.bat
```
→ Bấm nút **▶ START SERVER**

#### 5. Chạy Client

**Terminal 2:**
```cmd
run-client.bat
```
→ Đăng ký → Đăng nhập → Dùng

---

## 🎮 Hướng dẫn sử dụng

### 1️⃣ Đăng ký tài khoản

- Mở Client → Tab **"Tạo tài khoản"**
- Nhập:
  - Tên đăng nhập: `an`
  - Mật khẩu: `123456`
  - Xác nhận: `123456`
  - Server IP: `localhost` (hoặc IP máy server)
- Bấm **TẠO TÀI KHOẢN**
- Email tự động: `an@gmail.com`

### 2️⃣ Đăng nhập

- Tab **"Đăng nhập"**
- Nhập `an / 123456`
- Bấm **ĐĂNG NHẬP**
- → Vào giao diện hộp thư

### 3️⃣ Gửi thư cho người khác

- Sidebar trái → Bấm **✏️ Soạn thư**
- Điền:
  - Người nhận: `binh@gmail.com`
  - Tiêu đề: `Chào Bình`
  - Nội dung: `Đây là mail test`
- Bấm **📤 GỬI**

### 4️⃣ Xem thư đã nhận

- Sidebar → **📥 Hộp thư đến**
- **Click vào mail** → Nội dung hiện bên phải
- Toolbar: **Trả lời** · **Đánh dấu** · **Xóa**

### 5️⃣ Đọc thư đã gửi

- Sidebar → **📤 Đã gửi**

### 6️⃣ Xem danh bạ

- Sidebar → **📇 Danh bạ**
- **Đúp chuột** vào user → Mở form soạn thư với người nhận đã điền

---

## 📡 Giao thức UDP

### Định dạng request

Mỗi request là 1 chuỗi UTF-8, các trường cách nhau bằng dấu `|`:

```
COMMAND|arg1|arg2|arg3|...
```

### Danh sách lệnh

| Lệnh | Cú pháp | Mô tả |
|------|---------|-------|
| `REGISTER` | `REGISTER\|user\|pass` | Tạo tài khoản mới |
| `LOGIN` | `LOGIN\|user\|pass` | Đăng nhập |
| `SEND` | `SEND\|sender\|fromEmail\|toEmail\|subject\|content` | Gửi thư |
| `GET` | `GET\|user\|folder\|fileName` | Đọc nội dung mail |
| `STAR` | `STAR\|user\|folder\|fileName` | Đánh dấu / bỏ đánh dấu |
| `DELETE` | `DELETE\|user\|folder\|fileName` | Xóa mail |
| `SENT` | `SENT\|user` | Danh sách mail đã gửi |
| `TRASH` | `TRASH\|user` | Danh sách thùng rác |
| `STARRED` | `STARRED\|user` | Danh sách mail đánh dấu |
| `LIST` | `LIST` | Danh sách user đã đăng ký |

### Định dạng response

Bắt đầu bằng `OK|` (thành công) hoặc `ERROR|` (lỗi), sau đó là dữ liệu:

```
OK|Login successful
EMAIL|an@gmail.com
SECTION|INBOX
ACCOUNTFILE|user.txt
ACCOUNTFILE|pass.txt
MAIL|INBOX|email_binh_...txt|binh@gmail.com|an@gmail.com|Hello|2026-10-04|Nội dung|0
```

### Port mặc định

```
UDP Port: 2023
```

---

## 🌐 Test 2 máy LAN

### Bước 1: Xác định IP máy Server

Trên **máy Server**, mở CMD:

```cmd
ipconfig
```

Tìm dòng **IPv4 Address** (VD: `192.168.1.10`).

### Bước 2: Chạy Server trên Máy A

```cmd
run-server.bat
```
→ Bấm **START SERVER**

### Bước 3: Chạy Client trên Máy B

**Đảm bảo 2 máy cùng mạng WiFi/LAN.**

```cmd
run-client.bat
```

- Ô **Server IP** → nhập `192.168.1.10` (không phải `localhost`)
- Đăng ký / Đăng nhập
- Gửi thư → Server A lưu vào `F:\server_mail\`

### ⚠️ Lưu ý

- **Tắt Firewall** trên máy Server (hoặc mở port 2023):
  ```cmd
  netsh advfirewall firewall add rule name="MailServer UDP 2023" dir=in action=allow protocol=UDP localport=2023
  ```
- Đảm bảo 2 máy **cùng subnet** (VD: `192.168.1.x`)
- Nếu ping không được → kiểm tra router/firewall

---

## 🖼️ Screenshots

### 🔐 Login

![Login](docs/login.png)

### 📥 Inbox

![Inbox](docs/inbox.png)

### ✏️ Compose

![Compose](docs/compose.png)

### 🖥️ Server Dashboard

![Server](docs/server.png)

> 📝 **Ghi chú:** Screenshots sẽ được bổ sung sau. Hiện tại bạn có thể chạy trực tiếp để xem giao diện.

---

## 👨‍💻 Tác giả

**Chamm286**

- 🎓 Sinh viên môn **Lập trình mạng**
- 💻 GitHub: [@Chamm286](https://github.com/Chamm286)
- 📧 Email: *(cập nhật)*

---

## 📄 License

Dự án này được phân phối theo giấy phép **MIT License**.

Xem file [LICENSE](LICENSE) để biết chi tiết.

---

## 🙏 Cảm ơn

- Thầy **GVHD** — Hướng dẫn môn Lập trình mạng
- Cộng đồng [JavaFX](https://openjfx.io/) và [Ikonli](https://kordamp.org/ikonli/)
- [Shields.io](https://shields.io/) — Badge generator

---

## 📊 Thống kê dự án

![GitHub repo size](https://img.shields.io/github/repo-size/Chamm286/MailServerJavaFX?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/Chamm286/MailServerJavaFX?style=flat-square)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/Chamm286/MailServerJavaFX?style=flat-square)

---

<div align="center">

**⭐ Nếu bạn thấy project hữu ích, hãy cho 1 star nhé! ⭐**

Made with ❤️ by [Chamm286](https://github.com/Chamm286)

</div>