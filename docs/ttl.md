# TTL và phần tử hết hạn trong minimal-cache

[Eviction](eviction.md) chọn phần tử để loại khi cache đầy. TTL (*time to live*) trả lời một câu hỏi khác: một giá trị còn hợp lệ bao lâu sau khi được ghi vào cache?

## TTL hoạt động thế nào?

`TtlExpirationPolicy` nhận một `Duration` dương. Mỗi lần `put` một key, policy ghi thời điểm ghi vào trường `writeTime` trên `CacheEntry` của key đó, đọc từ một `Ticker`: nguồn thời gian trả về số nanosecond, mặc định là `System.nanoTime()`. Khi kiểm tra key, nó lấy thời gian đã trôi qua kể từ lần ghi gần nhất và so với TTL. Một lần `get` không gia hạn TTL; cập nhật giá trị bằng `put` sẽ ghi lại thời điểm và bắt đầu một khoảng TTL mới.

Ví dụ TTL là 5 phút: ghi A lúc 10:00, đọc A lúc 10:04 thì A vẫn hợp lệ; nếu không ghi lại A, một lần đọc ở 10:05 hoặc sau đó sẽ thấy A hết hạn. Các mốc giờ ở đây chỉ để minh họa; code đo khoảng thời gian đã trôi qua, không lưu giờ đồng hồ.

Nếu không gọi `ttl` trong DSL `Caches.create(...)`, cache dùng `NoExpirationPolicy`, nên phần tử không hết hạn theo thời gian.

Gọi `ticker(...)` trong DSL để thay nguồn thời gian. Test trong repo dùng một `FakeTicker` chỉ tăng thời gian khi gọi `advance(...)`, nên kiểm tra được entry hết hạn mà không phải `Thread.sleep`.

Nguồn trong repo: [TtlExpirationPolicy](../src/main/java/app/cache/expiration/ttl/TtlExpirationPolicy.java), [NoExpirationPolicy](../src/main/java/app/cache/expiration/ttl/NoExpirationPolicy.java), [CacheEntry](../src/main/java/app/cache/entry/CacheEntry.java), [Ticker](../src/main/java/app/cache/Ticker.java), [Caches](../src/main/java/app/cache/Caches.java), [FakeTicker](../src/test/java/app/cache/FakeTicker.java).

## Khi nào phần tử hết hạn thực sự bị xóa?

TTL ở đây được kiểm tra khi cache thực hiện thao tác, không có luồng chạy nền để dọn tự động:

- `get(key)` kiểm tra key đang có; nếu hết hạn thì xóa khỏi cache và eviction policy, rồi trả về `Optional.empty()`.
- `put(key, value)` với key cũ đã hết hạn sẽ xóa entry cũ trước khi ghi lại. Nếu cache đang đầy và cần thêm key mới, nó quét và dọn các entry hết hạn trước khi gọi eviction.
- `size()` chỉ trả số entry còn nằm trong map. Entry đã hết hạn nhưng chưa được kiểm tra vẫn có thể được tính vào số này.

Một điểm cần biết về API hiện tại: `remove(key)` không kiểm tra TTL. Nếu entry đã hết hạn nhưng chưa bị dọn, `remove` vẫn trả về giá trị vừa xóa. Vì vậy, đừng dùng `remove` như một cách kiểm tra giá trị còn hợp lệ.

Nguồn trong repo: [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java), [TtlExpirationPolicy](../src/main/java/app/cache/expiration/ttl/TtlExpirationPolicy.java).

## TTL phối hợp với eviction ra sao?

Expiration policy ghi thời điểm ghi lên entry; eviction policy giữ thứ tự hoặc tần suất để chọn key khi cache đầy. `TtlExpirationPolicy` không giữ trạng thái riêng, nên khi entry bị xóa vì TTL, `InMemoryCache` chỉ cần báo cho eviction policy bằng `onRemove(entry)`. Nếu sau khi dọn entry hết hạn cache vẫn đầy, nó mới gọi `evict()` theo FIFO, LRU hoặc LFU. Hai cơ chế có trách nhiệm riêng nhưng cùng làm việc trên các entry trong map duy nhất của cache.

Nguồn trong repo: [InMemoryCache](../src/main/java/app/cache/InMemoryCache.java), [ExpirationPolicy](../src/main/java/app/cache/expiration/ExpirationPolicy.java), [EvictionPolicy](../src/main/java/app/cache/eviction/EvictionPolicy.java).
