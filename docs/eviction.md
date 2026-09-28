# Eviction và các chính sách loại bỏ

Đọc [cache trong bộ nhớ](cache.md) trước để biết `capacity` và các thao tác cơ bản. **Eviction** là việc loại một entry để nhường chỗ khi thêm key mới vào cache đã đầy. Trước khi eviction, nếu cache có cấu hình TTL, `InMemoryCache` dọn entry hết hạn; nếu vẫn đầy, nó hỏi eviction policy key nào cần loại.

## Eviction policy giữ thông tin gì?

`InMemoryCache` giữ dữ liệu trong **một** `HashMap<K, CacheEntry<K, V>>`. Mỗi `CacheEntry` chứa key, value và các thông tin policy cần: thời điểm ghi cho TTL, tần suất cho LFU, và hai liên kết `prev`/`next` để nối entry vào một danh sách liên kết đôi. Danh sách đó là `EntryList`, dùng chung cho cả ba policy: thêm vào cuối, tháo một entry, lấy entry đầu tiên và chuyển một entry xuống cuối đều chỉ đổi vài liên kết.

Vì policy nhận thẳng entry thay vì key, nó không cần map riêng để tìm entry theo key; mỗi thao tác cache chỉ tra hash một lần. Mỗi cache chỉ có một eviction policy, nên `prev`/`next` của một entry chỉ thuộc về một danh sách tại một thời điểm.

Giao diện `EvictionPolicy` mô tả các sự kiện của entry: `onAdd` khi key mới vào cache, `onAccess` khi `get` trả về giá trị còn hiệu lực hoặc `put` cập nhật key đang có, `onRemove` khi entry bị xóa, `evict` để chọn và tháo entry cần loại, và `clear` để xóa toàn bộ trạng thái. Khi entry bị xóa do TTL, cache cũng gọi `onRemove` để policy tháo entry khỏi danh sách của nó.

Nguồn trong repo: [EvictionPolicy](../src/main/java/app/cache/eviction/EvictionPolicy.java), [CacheEntry](../src/main/java/app/cache/entry/CacheEntry.java), [EntryList](../src/main/java/app/cache/entry/EntryList.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Ba chính sách hiện có

| Chính sách | Key bị loại khi cache đầy | Điều làm thay đổi mức ưu tiên |
| --- | --- | --- |
| [FIFO](fifo.md) | Key được thêm vào sớm nhất trong các key còn lại | Thêm key mới; đọc và cập nhật key cũ không đổi thứ tự. |
| [LRU](lru.md) | Key lâu nhất chưa được sử dụng | Đọc hoặc ghi một key đưa nó thành key dùng gần nhất. |
| [LFU](lfu.md) | Key có tần suất sử dụng thấp nhất | Đọc hoặc cập nhật key làm tăng tần suất; nếu bằng nhau, xét thứ tự vào nhóm tần suất. |

Ví dụ với `capacity = 3`: sau khi thêm A, B, C rồi đọc A, lần thêm D sẽ loại A theo FIFO nhưng loại B theo LRU. LFU còn phụ thuộc số lần từng key đã được đọc hoặc cập nhật. Các trang riêng ở bảng trên theo dõi từng bước và giải thích cấu trúc dữ liệu của mỗi policy.

TTL không phải một eviction policy: nó xác định entry hết hạn theo thời gian. Cache dọn entry hết hạn trước khi cần loại một key còn hợp lệ để nhường chỗ. Xem [TTL](ttl.md) để biết khi nào việc kiểm tra và dọn thực sự diễn ra.

Nguồn trong repo: [FifoEvictionPolicy](../src/main/java/app/cache/eviction/fifo/FifoEvictionPolicy.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).
