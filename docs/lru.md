# Chính sách LRU trong minimal-cache

Tài liệu này đi sâu vào LRU, tiếp nối [tổng quan eviction](eviction.md). Nếu chưa quen với `capacity` và các thao tác cache, đọc [cache trong bộ nhớ](cache.md) trước.

## LRU chọn phần tử nào?

LRU (*Least Recently Used*) loại key **lâu nhất chưa được sử dụng**. Trong implementation hiện tại, đọc một key còn hiệu lực bằng `get` hoặc thêm/cập nhật bằng `put` đều tính là sử dụng key đó. Key được sử dụng gần đây nhất nằm cuối thứ tự; key lâu nhất nằm đầu và sẽ bị loại tiếp theo.

Giả sử `capacity = 3`, không cấu hình TTL và ban đầu cache rỗng. `put(A)` trong bảng là cách viết gọn cho `put(A, giá trị)`:

| Thao tác | Thứ tự LRU, từ lâu nhất đến gần nhất | Điều xảy ra |
| --- | --- | --- |
| `put(A)`, `put(B)`, `put(C)` | `A → B → C` | Cache vừa đầy. |
| `get(A)` | `B → C → A` | A được chuyển xuống cuối vì vừa được đọc. |
| `put(D)` | `C → A → D` | B bị loại để nhường chỗ cho D. |
| `put(A)` | `C → D → A` | Cập nhật A làm A trở thành key dùng gần nhất. |

Với cùng ba thao tác đầu, FIFO sẽ loại **A** khi thêm D vì A được đưa vào sớm nhất; LRU loại **B** vì A vừa được đọc. Đây là khác biệt cốt lõi giữa hai chính sách.

Nguồn trong repo: [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java), [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java).

## Repo giữ thứ tự sử dụng bằng gì?

`LruEvictionPolicy` giữ các entry trong một `EntryList` (xem [tổng quan eviction](eviction.md)): đầu danh sách là key lâu nhất chưa được dùng, cuối danh sách là key vừa được dùng gần nhất.

Khi một key được sử dụng lại, `moveToLast` tháo entry khỏi vị trí cũ rồi gắn vào cuối danh sách. Khi cần eviction, `removeFirst` tháo entry ở đầu; `InMemoryCache` sau đó xóa key của entry đó khỏi map. Policy không có map riêng: `InMemoryCache` tra key một lần rồi đưa thẳng entry cho policy.

Nguồn trong repo: [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [EntryList](../src/main/java/app/cache/entry/EntryList.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Các thao tác tác động đến LRU ra sao?

- `onAdd(entry)` thêm entry mới vào cuối.
- `onAccess(entry)` chuyển entry xuống cuối. `InMemoryCache` gọi thao tác này khi `get` trả về giá trị còn hiệu lực và khi `put` cập nhật key đang có.
- `onRemove(entry)` tháo entry khi key bị xóa thủ công hoặc hết hạn.
- `evict()` tháo entry ở đầu danh sách; `clear()` xóa toàn bộ thứ tự liên kết.

Điểm dễ nhầm: một lần `get` không tìm thấy key không làm đổi thứ tự LRU. Tương tự, key đã hết hạn bị xóa khỏi cache và policy thay vì được chuyển xuống cuối. Việc chuyển entry và loại entry đầu danh sách chỉ cần thay đổi một số liên kết; tra key trong `HashMap` của cache có chi phí trung bình hằng số. Đây là đặc điểm của cách cài đặt hiện tại, chưa xét truy cập đồng thời.

Nguồn trong repo: [LruEvictionPolicy](../src/main/java/app/cache/eviction/lru/LruEvictionPolicy.java), [EntryList](../src/main/java/app/cache/entry/EntryList.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [Chính sách LFU](lfu.md) để xem cách chọn key theo tần suất sử dụng.
