# Cache trong bộ nhớ là gì?

`minimal-cache` là một cache trong bộ nhớ: nó giữ các cặp **key–value** để chương trình đọc lại bằng key. Dữ liệu nằm trong `HashMap` của `InMemoryCache` và chỉ tồn tại trong vòng đời của instance cache này. Đây là dự án học tập về cấu trúc và luồng hoạt động của cache; project chưa có thống kê cache hit/miss và chưa hỗ trợ truy cập đồng thời.

## Các thao tác cơ bản

- `put(key, value)` thêm key mới hoặc thay giá trị của key đã có.
- `get(key)` trả về `Optional` chứa giá trị nếu key còn hiệu lực, hoặc `Optional.empty()` nếu không có giá trị để trả.
- `remove(key)` xóa key và trả về giá trị vừa xóa qua `Optional`.
- `size()` trả số entry còn nằm trong map; `clear()` xóa toàn bộ entry.

Cache không nhận key hoặc value `null`. Nếu dùng TTL, `get` còn kiểm tra thời hạn trước khi trả giá trị; entry hết hạn sẽ bị xóa khi được kiểm tra. Vì việc dọn hết hạn không chạy nền, `size()` vẫn có thể tính một entry đã hết hạn nhưng chưa được dọn.

Nguồn trong repo: [Cache](../src/main/java/app/cache/Cache.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Giới hạn số phần tử

Mỗi cache có một `capacity` lớn hơn 0. Khi thêm key mới vào cache đã đầy và cache có cấu hình TTL, `InMemoryCache` dọn các entry hết hạn trước; không có TTL thì bỏ qua bước này. Nếu sau đó vẫn đầy, cache cần chọn một key để loại rồi mới thêm key mới. Quyết định chọn key thuộc về **eviction policy**; map chứa giá trị không tự quyết định thứ tự loại bỏ.

TTL và eviction giải quyết hai việc khác nhau. TTL xác định entry còn hợp lệ theo thời gian; eviction tạo chỗ trống khi cache đầy. Cache có thể dùng eviction mà không bật TTL.

Nguồn trong repo: [CacheConfig](../src/main/java/app/cache/config/CacheConfig.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java), [Caches](../src/main/java/app/cache/Caches.java).

Đọc tiếp: [Eviction và các chính sách loại bỏ](eviction.md). Nếu muốn dùng API ngay, xem [hướng dẫn sử dụng](usage.md).
