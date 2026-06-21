[English](README_EN.md)

# DictEditor - Trình chỉnh sửa từ điển Android giao diện Material 3

DictEditor là một ứng dụng chỉnh sửa từ điển Android hiện đại, cao cấp được xây dựng bằng Jetpack Compose, Material 3 và kiến trúc MVVM. Ứng dụng được thiết kế để quản lý các mục từ điển Trung-Việt với khả năng chỉnh sửa nâng cao, sắp xếp lại thứ tự nghĩa bằng cách kéo thả và tích hợp dịch thuật hỗ trợ bởi AI.

## Tính năng

- **Giao diện Material 3 UI/UX**: Bố cục rõ ràng, mượt mà với các yếu tố thiết kế kính mờ (glassmorphism) và thông báo hiển thị ở phía trên màn hình.
- **Quản lý nghĩa của từ**: Hỗ trợ chỉnh sửa trực tiếp, thêm mới, xóa và kéo thả để sắp xếp lại thứ tự của các nghĩa của từ.
- **Tích hợp AI Translate Portal (ATP)**: Kết nối với các dịch vụ dịch thuật bên ngoài thông qua kết nối AIDL.
- **Nhập & Xuất dữ liệu**: Dễ dàng nhập hàng loạt từ mới và xuất file từ điển của bạn.
- **Hoàn tác & Làm lại (Undo & Redo)**: Hỗ trợ đầy đủ việc hoàn tác và làm lại các chỉnh sửa để tránh mất mát dữ liệu.

## Công nghệ sử dụng

- **Framework**: Android SDK (Kotlin), Jetpack Compose
- **Design System**: Material Design 3
- **Kiến trúc**: MVVM (Model-View-ViewModel) với StateFlow
- **Đồng thì (Concurrency)**: Kotlin Coroutines & Flow
- **IPC**: AIDL (Android Interface Definition Language)

## Bắt đầu sử dụng

### Yêu cầu hệ thống

- Android Studio Koala hoặc mới hơn
- Android SDK 34+
- Java JDK 17

### Hướng dẫn Build và Chạy thử

1. Clone repository này về máy.
2. Mở thư mục dự án bằng Android Studio.
3. Chờ Gradle đồng bộ và tải các thư viện phụ thuộc.
4. Chạy ứng dụng trên Emulator Android hoặc thiết bị vật lý (khuyến nghị API level 26 trở lên).