# Chính sách LFU trong minimal-cache

Tài liệu này đi sâu vào LFU, tiếp nối [tổng quan eviction](eviction.md). Cache vẫn giới hạn số phần tử bằng `capacity`; LFU thay đổi cách chọn key bị loại khi cache đầy.

## LFU chọn phần tử nào?

LFU (*Least Frequently Used*) loại key có **tần suất sử dụng thấp nhất**. Trong repo này, key mới bắt đầu ở tần suất 1. Mỗi lần `get` một giá trị còn hiệu lực hoặc `put` cập nhật một key đang có sẽ tăng tần suất của key đó thêm 1. Nếu nhiều key cùng tần suất thấp nhất, key vào nhóm tần suất đó sớm nhất bị loại trước.

Giả sử `capacity = 3`, không cấu hình TTL và cache ban đầu rỗng. `put(A)` là cách viết gọn cho `put(A, giá trị)`:

| Thao tác | Nhóm tần suất sau thao tác, theo thứ tự cũ → mới | Điều xảy ra |
| --- | --- | --- |
| `put(A)`, `put(B)`, `put(C)` | `1: A → B → C` | Cả ba key có tần suất 1. |
| `get(A)`, `get(B)` | `1: C`; `2: A → B` | A và B được đọc, mỗi key tăng lên 2. |
| `put(D)` | `1: D`; `2: A → B` | C có tần suất thấp nhất nên bị loại. |
| `get(D)` | `2: A → B → D` | D chuyển sang nhóm tần suất 2. |
| `put(E)` | `1: E`; `2: B → D` | Cả A, B, D cùng tần suất 2; A vào nhóm này sớm nhất nên bị loại. |

Nguồn trong repo: [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

## Repo giữ tần suất và thứ tự ra sao?

Tần suất của mỗi key nằm ngay trên `CacheEntry` của nó (xem [tổng quan eviction](eviction.md)). `LfuFrequencyStructure` có map `buckets` để tìm nhóm theo tần suất, và `minFreq` để biết nhóm nào bị xét trước. Mỗi nhóm là một `EntryList`: đầu nhóm là entry vào nhóm sớm nhất, cuối nhóm là entry mới vào nhóm gần đây nhất. Map `buckets` có khóa là tần suất, không phải key của cache.

Khi key được sử dụng, code tháo entry khỏi bucket cũ, tăng tần suất, rồi gắn entry vào cuối bucket mới. Khi eviction, code lấy entry đầu tiên của bucket `minFreq`. Vì vậy, quy tắc hòa là **thứ tự vào nhóm tần suất hiện tại**, không phải thứ tự được thêm lần đầu vào cache.

Nguồn trong repo: [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [CacheEntry](../src/main/java/app/cache/entry/CacheEntry.java), [EntryList](../src/main/java/app/cache/entry/EntryList.java).

## Các thao tác tác động đến LFU ra sao?

- `onAdd(entry)` thêm key mới với tần suất 1.
- `onAccess(entry)` tăng tần suất khi cache trả về một giá trị còn hiệu lực hoặc khi `put` cập nhật key đang có. Đọc key không tồn tại hoặc đã hết hạn không làm tăng tần suất.
- `onRemove(entry)` tháo entry khỏi nhóm tần suất khi key bị xóa thủ công hoặc do hết hạn.
- `evict()` lấy entry ở đầu bucket có `minFreq`; `clear()` xóa toàn bộ bucket và đặt lại `minFreq`.

Đây là tần suất tích lũy trong thời gian key còn ở cache, chưa có cơ chế giảm tần suất theo thời gian. `findMinFrequency()` quét các bucket hiện có khi cần tìm lại mức thấp nhất, nên không nên mặc định rằng mọi thao tác của implementation này đều có chi phí hằng số. Repo cũng chưa hỗ trợ truy cập đồng thời.

Nguồn trong repo: [LfuEvictionPolicy](../src/main/java/app/cache/eviction/lfu/LfuEvictionPolicy.java), [LfuFrequencyStructure](../src/main/java/app/cache/eviction/lfu/LfuFrequencyStructure.java), [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java).

Đọc tiếp: [TTL và cách cache xử lý phần tử hết hạn](ttl.md).
