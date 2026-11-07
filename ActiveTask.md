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

### 3.2. Đường Ống Xử Lý Sát Thương & Làm Lại Cơ Chế Giáp (Damage Pipeline)
Cơ chế giáp gốc của Minecraft và bùa Bảo vệ đã được làm lại hoàn toàn theo chuẩn MOBA (Hyperbolic Formula):
- **Files xử lý:** 
  - [`com.valor.items.event.RankbreakerEvents`](src/main/java/com/valor/items/event/RankbreakerEvents.java)
  - [`com.valor.items.item.RankbreakerItem`](src/main/java/com/valor/items/item/RankbreakerItem.java)
- **Cách thức can thiệp:**
  1. `LivingHurtEvent` (`priority = LOWEST`): Lưu trữ lượng sát thương vật lý gốc trước giáp (`ValorRawDamage`).
  2. `LivingDamageEvent` (`priority = HIGHEST`): Chạy trước các hiệu ứng giáp ảo (`HerculesEvents`), ghi đè hoàn toàn công thức giáp và bùa Bảo vệ vanilla bằng công thức mới.
- **Quy tắc tính toán Giáp & Bùa Bảo Vệ:**
  1. **Quy đổi Bùa Bảo Vệ (Protection):** Mỗi cấp độ bùa Bảo vệ trên trang bị tăng **+2.5% giáp** (Cấp 1 = +2.5%, Cấp 2 = +5%, Cấp 3 = +7.5%, Cấp 4 = +10%). Full 4 món Protection IV = **+40% giáp**:
     $$\text{totalArmor} = \text{baseArmor} \times (1.0 + \text{totalProtLevels} \times 0.025)$$
  2. **Trừ giáp theo thứ tự LMHT:**
     $$\text{effectiveArmor} = \max\Big(0,\; \text{totalArmor} \times (1.0 - \% \text{xuyên giáp}) - \text{Sát lực}\Big)$$
  3. **Công thức cản sát thương Hyperbolic (Hướng 1 - $K = 6.0$):**
     $$\text{finalDamage} = \text{rawDamage} \times \left(\frac{6.0}{6.0 + \text{effectiveArmor}}\right)$$
  4. **Bảo tồn hiệu ứng thuốc Kháng sát thương (Resistance):** Nếu mục tiêu có hiệu ứng `DAMAGE_RESISTANCE`, sát thương tiếp tục nhân với $(1.0 - \text{level} \times 0.2)$.
- **Cộng dồn Sát Lực từ Rankbreaker:**
  - `RankbreakerItem.getTotalLethality(player)` duyệt toàn bộ slot Curios và cộng dồn toàn bộ Sát lực của tất cả các bản sao Thương Xuyên Phá đang đeo (Ví dụ đeo 6 cái cấp 5 max sẽ được $13 \times 6 = 78$ Sát lực).

---

## 4. Chi Tiết Trạng Thái Vừa Thay Đổi (Recent Task State)

### Đã thay đổi:
1. **Làm lại công thức Giáp & Bùa Bảo vệ (`RankbreakerEvents.java`):**
   - Áp dụng công thức hyperbolic $K / (K + \text{effectiveArmor})$ với $K = 6.0$.
   - Bùa Bảo Vệ (Protection) được chuyển thành bonus **+2.5% giáp/cấp** (+40% cho full set Protection IV).
   - Khi người chơi có đủ Sát lực (ví dụ 78 Sát lực) vượt qua tổng giáp (kể cả giáp sau khi buff bùa Bảo vệ), sát thương gây ra đạt $100\%$ raw damage (không còn bị bùa Bảo vệ giảm oan 64%).
2. **Cộng dồn Sát Lực (`RankbreakerItem.java`):**
   - Thêm phương thức `getTotalLethality(LivingEntity)` tính tổng Sát lực từ tất cả các Rankbreaker đang đeo thay vì chỉ lấy cái cao nhất.
3. **Đổi tên attribute & thêm % Xuyên giáp:**
   - Đổi `ARMOR_PENETRATION` $\rightarrow$ `LETHALITY` (`valor_items:lethality`).
   - Thêm `ARMOR_PENETRATION_PERCENT` (`valor_items:armor_penetration_percent`).
   - Giữ alias `@Deprecated ARMOR_PENETRATION = LETHALITY`.
4. **Bản dịch (Localization):**
   - Đã cập nhật `en_us.json` và `vi_vn.json` cho `lethality` ("Sát lực") và `armor_penetration_percent` ("Xuyên giáp (%)").

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
