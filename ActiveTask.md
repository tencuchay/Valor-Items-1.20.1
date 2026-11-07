# AGENT HANDOVER & ACTIVE TASK CONTEXT: VALOR ITEMS (1.20.1)

> **Mục đích tài liệu:** Hướng dẫn chi tiết dành cho các AI Agent (hoặc Developer) tiếp quản dự án. Đọc tài liệu này để hiểu toàn bộ kiến trúc hệ thống, quy ước code, cơ chế tính toán chỉ số, và quy trình từng bước khi thêm/sửa đổi tính năng mà không làm hỏng logic hiện có.

---

## 1. Thông Tin Kỹ Thuật (Tech Stack & Environment)
- **Game Version:** Minecraft 1.20.1
- **Mod Loader:** Minecraft Forge `47.3.0` (Mapping: `official 1.20.1`)
- **Java:** JDK 17
- **Mod ID:** `valor_items`
- **Các dự án liên kết trong cùng workspace:**
  - `Valor-Items-1.20.1` (Thư mục hiện tại): Mod chính chứa vật phẩm, trang bị (Curios), chỉ số nhân vật (Attributes) và logic chiến đấu.
  - `Valor-Indicator-1.20.1` (`D:\Valor-Indicator-1.20.1`): Mod hiển thị UI, thanh máu và bảng chỉ số phím `U` (`StatsScreen`). Đọc attribute động từ `valor_items` thông qua `ForgeRegistries.ATTRIBUTES`.
  - `Valor-Weapons-1.20.1` (`D:\Valor-Weapons-1.20.1`): Mod vũ khí mở rộng.

---

## 2. Quy Tắc Ứng Xử Bắt Buộc (Tuân thủ theo `README.md`)
Bất kỳ AI Agent nào khi can thiệp codebase này đều **phải tuân thủ nghiêm ngặt 4 quy tắc**:
1. **Think Before Coding:** Nêu rõ giả định trước khi làm. Nếu có nhiều cách giải thích hoặc điểm chưa rõ, phải dừng lại hỏi người dùng trước khi code.
2. **Simplicity First:** Viết lượng code tối thiểu giải quyết đúng bài toán. Không tự tạo abstraction cho code dùng 1 lần, không tạo cấu hình thừa thãi.
3. **Surgical Changes:** Chỉ sửa đúng vị trí cần thiết. Giữ nguyên format/style xung quanh. Không tự ý refactor code không hỏng. Tự dọn sạch import/biến do mình làm thừa ra.
4. **Goal-Driven Execution:** Xác định rõ tiêu chí kiểm chứng (verification criteria). Trước khi kết thúc phải chạy lệnh kiểm tra (ví dụ: `./gradlew.bat build`).

---

## 3. Kiến Trúc & Cơ Chế Cốt Lõi Của Hệ Thống

### 3.1. Hệ thống Thuộc Tính (Custom Attributes)
- **Khai báo:** Tất cả chỉ số mở rộng của mod đều nằm tại:
  [`com.valor.items.registry.ModAttributes`](src/main/java/com/valor/items/registry/ModAttributes.java)
- **Đăng ký cho Thực thể:** Được đăng ký vào `EntityType.PLAYER` thông qua sự kiện Forge:
  `EntityAttributeModificationEvent` trong class `ModAttributes`.
- **Đồng bộ Client-Server:** Hầu hết đều gọi `.setSyncable(true)` để client (như HUD/bảng chỉ số) có thể đọc được giá trị hiện tại.

### 3.2. Đường Ống Xử Lý Sát Thương & Xuyên Giáp (Damage Pipeline)
Minecraft Vanilla **không có** cơ chế trừ giáp từng phần (chỉ có bypass 100% giáp). Do đó, mod can thiệp vào:
- **File xử lý:** [`com.valor.items.event.RankbreakerEvents`](src/main/java/com/valor/items/event/RankbreakerEvents.java)
- **Event:** `LivingHurtEvent` với `@SubscribeEvent(priority = EventPriority.LOWEST)` (chạy sau cùng để các hiệu ứng tăng giảm damage khác đã tính xong).
- **Thứ tự ưu tiên tính Giáp (Chuẩn LMHT):**
  1. `% Xuyên giáp` (`ARMOR_PENETRATION_PERCENT`): Bỏ qua một phần trăm giáp gốc của nạn nhân.
  2. `Sát lực` (`LETHALITY`): Trừ tiếp vào lượng giáp phẳng còn lại.
  3. `Armor Toughness`: Giữ nguyên công thức giảm trừ của Minecraft vanilla.
- **Công thức tính toán:**
  ```java
  double effectiveArmor = armor * Math.max(0.0D, 1.0D - percent) - lethality;
  float finalArmor = (float) Math.max(0.0D, effectiveArmor);
  float wantedDamage = CombatRules.getDamageAfterAbsorb(amount, finalArmor, toughness);
  ```
- **Thuật toán tìm kiếm nhị phân (Binary Search):** Vì Minecraft sẽ tự động tính giáp gốc sau `LivingHurtEvent`, thuật toán sẽ nâng `event.setAmount(high)` lên mức vừa đủ để sau khi trừ giáp gốc của game, sát thương thực nhận đúng bằng `wantedDamage`.

---

## 4. Chi Tiết Trạng Thái Vừa Thay Đổi (Recent Task State)

### Đã thay đổi ngày 2026-11-07:
1. **Đổi tên attribute xuyên giáp phẳng:**
   - Cũ: `ARMOR_PENETRATION` (`valor_items:armor_penetration`).
   - Mới: **`LETHALITY`** (`valor_items:lethality`).
   - Giữ alias tương thích ngược:
     `@Deprecated public static final RegistryObject<Attribute> ARMOR_PENETRATION = LETHALITY;`
2. **Thêm attribute mới:**
   - **`ARMOR_PENETRATION_PERCENT`** (`valor_items:armor_penetration_percent`).
   - Khoảng giá trị: `0.0D` đến `1.0D` (tương ứng $0\% \to 100\%$).
3. **Cập nhật logic sát thương:**
   - Class `RankbreakerEvents`:
     - Trang bị Thương Xuyên Phá (Rankbreaker) gán điểm xuyên giáp vào modifier `LETHALITY`.
     - Hàm `onHurt` đọc cả hai attribute `LETHALITY` và `ARMOR_PENETRATION_PERCENT` để tính giáp hiệu dụng theo công thức LMHT nêu trên.
4. **Bản dịch (Localization):**
   - Đã cập nhật cả hai file `en_us.json` và `vi_vn.json`:
     - `attribute.valor_items.lethality`: "Lethality" / "Sát lực"
     - `attribute.valor_items.armor_penetration_percent`: "Armor Penetration (%)" / "Xuyên giáp (%)"

---

## 5. Quy Trình Chuẩn Dành Cho Agent Khi Muốn Thêm / Sửa Đổi Một Chỉ Số Mới

Khi nhận nhiệm vụ thêm một Stat mới (ví dụ: Tốc đánh, Hút máu phép, Kháng hiệu ứng...), hãy làm đúng theo 5 bước sau:

### Bước 1: Khai báo Attribute trong `ModAttributes.java`
```java
public static final RegistryObject<Attribute> TEN_STAT = ATTRIBUTES.register("ten_stat",
        () -> new RangedAttribute("attribute.valor_items.ten_stat", defaultValue, minValue, maxValue).setSyncable(true));
```
Và thêm vào method `onAttributeModification`:
```java
event.add(EntityType.PLAYER, TEN_STAT.get());
```

### Bước 2: Thêm ngôn ngữ trong Resource Files
- Thêm key vào:
  - `src/main/resources/assets/valor_items/lang/en_us.json`
  - `src/main/resources/assets/valor_items/lang/vi_vn.json`
- Key format: `"attribute.valor_items.ten_stat": "Tên Hiển Thị"`

### Bước 3: Viết logic xử lý (Gameplay Logic)
- Nếu tác động vào chiến đấu: Đặt trong package `com.valor.items.event.*` (ví dụ `CombatEvents`, `RankbreakerEvents`, v.v.).
- Sử dụng getter: `attacker.getAttribute(ModAttributes.TEN_STAT.get())`.

### Bước 4: (Tùy chọn) Hiển thị lên màn hình phím `U` của `Valor-Indicator`
- Mở `D:\Valor-Indicator-1.20.1\src\main\java\com\tencuchay\valorindicator\client\StatsScreen.java`.
- Khai báo:
  ```java
  private static final Stat TEN_STAT = new Stat("ten_stat", () -> modded("valor_items", "ten_stat"), scale, isPercent, showBreakdown);
  ```
- Đưa vào danh sách `COLUMN_1` hoặc `COLUMN_2`.
- Thêm key vào file lang của `Valor-Indicator`: `"stat.valor_indicator.ten_stat": "Tên Hiển Thị"`.

### Bước 5: Build kiểm tra
- Chạy: `./gradlew.bat build`
- Đảm bảo terminal trả về `BUILD SUCCESSFUL` với `0 errors`.

---

## 6. Lưu Ý Khi Chạy Build Trong Môi Trường Sandbox
- Trên Windows, Gradle Wrapper có thể cần truy cập Gradle cache tại `D:\Gradle` hoặc `.gradle-user`.
- Nếu gặp lỗi `Access is denied` liên quan đến wrapper `.zip.lck` trong chế độ Sandbox, lệnh Gradle build cần được chạy ở chế độ cho phép truy cập filesystem bên ngoài (`BypassSandbox: true`).
