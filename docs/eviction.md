# Eviction và các chính sách loại bỏ

Đọc [cache trong bộ nhớ](cache.md) trước để biết `capacity` và các thao tác cơ bản. **Eviction** là việc loại một entry để nhường chỗ khi thêm key mới vào cache đã đầy. Trước khi eviction, `InMemoryCache` dọn entry hết hạn nếu có; nếu vẫn đầy, nó hỏi eviction policy key nào cần loại.

## Eviction policy giữ thông tin gì?

`InMemoryCache` giữ key và value trong một `HashMap`. Eviction policy chỉ theo dõi thông tin cần để chọn key: thứ tự được thêm, thứ tự sử dụng hoặc tần suất sử dụng. Hai bên phải cùng cập nhật khi key được thêm, đọc, xóa hoặc bị loại.

Giao diện `EvictionPolicy` mô tả các sự kiện đó: `onPut` cho lần ghi, `onGet` cho lần đọc, `onRemove` cho lần xóa, `evict` để chọn và bỏ key khỏi trạng thái policy, và `clear` để xóa toàn bộ trạng thái. `InMemoryCache` gọi `onGet` sau khi xác nhận giá trị còn hiệu lực; khi entry bị xóa do TTL, cache cũng gọi `onRemove` để policy không giữ key cũ.

Nguồn trong repo: [EvictionPolicy](../src/main/java/app/cache/eviction/EvictionPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Ba chính sách hiện có

| Chính sách | Key bị loại khi cache đầy | Điều làm thay đổi mức ưu tiên |
| --- | --- | --- |
| [FIFO](fifo.md) | Key được thêm vào sớm nhất trong các key còn lại | Thêm key mới; đọc và cập nhật key cũ không đổi thứ tự. |
| [LRU](lru.md) | Key lâu nhất chưa được sử dụng | Đọc hoặc ghi một key đưa nó thành key dùng gần nhất. |
| [LFU](lfu.md) | Key có tần suất sử dụng thấp nhất | Đọc hoặc cập nhật key làm tăng tần suất; nếu bằng nhau, xét thứ tự vào nhóm tần suất. |

Ví dụ với `capacity = 3`: sau khi thêm A, B, C rồi đọc A, lần thêm D sẽ loại A theo FIFO nhưng loại B theo LRU. LFU còn phụ thuộc số lần từng key đã được đọc hoặc cập nhật. Các trang riêng ở bảng trên theo dõi từng bước và giải thích cấu trúc dữ liệu của mỗi policy.

TTL không phải một eviction policy: nó xác định entry hết hạn theo thời gian. Cache dọn entry hết hạn trước khi cần loại một key còn hợp lệ để nhường chỗ. Xem [TTL](ttl.md) để biết khi nào việc kiểm tra và dọn thực sự diễn ra.

Nguồn trong repo: [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).
