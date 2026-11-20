# 附近优惠（Android APK 工程）

基于**官方开放接口**的周边商家与有效优惠检索应用。Kotlin + Jetpack Compose + Retrofit，Gradle Kotlin DSL。

> 当前交付的是可直接用 Android Studio 打开并编译的完整源码工程，不是预编译 APK（本机无 JDK / Android SDK）。构建方法见下方。

## 功能

| 需求 | 实现位置 |
| --- | --- |
| 位置检索 / 周边商家 | `data/location/AppLocationManager.kt`、`data/repository/NearbyRepository.kt` 调用美团地图开放平台 `/v1/search/nearby` |
| 半径筛选 1–5 km 可选 | `core/AppConfig.kt` 提供 `[1,2,3,5]`；UI 为 FilterChip；请求参数 `radius` 与客户端 Haversine 双重校验（`domain/NearbyInteractor.kt`） |
| 折扣优惠 | `data/remote/PromotionProvider.kt`，服务端桥接官方授权接口后返回，客户端不接触 appSecret / accessToken |
| 优惠有效期校验 | `domain/DiscountValidator.kt`：状态、启用标志、库存、开始/结束时间、ISO-8601 与秒/毫秒时间戳 |
| 自动过滤过期优惠 / 剔除失效活动 | 同上；已过期、已下线、已售罄、无明确结束时间的活动不进入列表，并按 ID 去重 |
| 有效优惠信息展示 | `ui/NearbyScreen.kt`：商家卡片展示折扣、标题与「有效期至」 |
| 自己选位置 / 地图选点 | `MapPickerActivity.kt`：高德官方 SDK 地图，拖动取中心点 + 逆地理编码出地址；可一键切回 GPS |
| 合规限制 | 见下节 |

## 合规限制（硬性边界）

本工程**只使用官方开放平台接口**，明确禁止以下行为，代码中也没有相应实现：

- ❌ 逆向 APK、反编译、Hook、绕过签名或安全校验
- ❌ 网络爬虫 / 批量抓取 / 缓存转存美团内容
- ❌ 抓包、MITM 代理回放、伪造 UA、伪造 IP 绕过校验
- ❌ 使用他人账号凭据、AppKey 或超出授权范围的接口
- ❌ 收集与周边检索无关的个人信息、后台持续定位

对应依据：美团《平台在线服务协议》第 5、6 条禁止爬虫/抓包/绕过安全机制；《合规分管理规则》将「非法爬取」「绕过安全保护机制」「身份冒用」列为扣 10 分行为。参见：
- <https://developer.meituan.com/docs/biz/comm-dev-rule-7>
- <https://developer.meituan.com/docs/biz/biz_xn202005112_4f476ff2-2005-4525-b42b-75fc0db4b3bd>
- 周边搜索接口文档：<https://lbs.meituan.com/support/search>

### 代码层面的合规措施

1. **域名白名单**：`HostAllowlistInterceptor` 只允许 `lbsapi.meituan.com` 与已配置的 HTTPS 桥接域名，杜绝请求任何非官方数据源。
2. **禁止明文**：`network_security_config.xml` 设置 `cleartextTrafficPermitted="false"`。
3. **抗抓包**：仅信任系统证书（不信任用户安装的 CA），可阻断常规代理证书 MITM。
4. **环境校验**：`ComplianceGuard` 检测 root 与（正式包）调试器附加，不通过则拒绝发起任何请求。
5. **密钥不入库**：Key 与桥接地址来自 `local.properties`，`.gitignore` 已排除；正式包不启用演示数据。
6. **最小权限**：仅申请 INTERNET、前台定位与网络状态（地图选点所需），不申请后台定位、存储、通讯录；`allowBackup=false`；R8 开启混淆。

### 关于地图选点的合规边界

「自己选位置」使用**高德地图官方 Android SDK**（`com.amap.api:3dmap-location-search`），通过 `mavenCentral()` 拉取固定版本，不抓包、不逆向、不访问任何非官方瓦片/数据。需说明两点：

- 高德 SDK 使用**自己的网络栈**请求其官方域名，不受本工程 OkHttp 域名白名单拦截器的约束；但它同样是官方授权 SDK，且工程全局 `network_security_config` 仍强制 HTTPS、仅信任系统证书，可阻断常规代理抓包。
- 未配置高德 Key 时，地图选点页会提示并安全退出，主界面 GPS 检索不受影响。

## 申请高德 Key（地图选点用，参数已固定）

地图选点需要在[高德开放平台控制台](https://console.amap.com/)申请一个 Android Key。由于 debug 签名已固定在仓库的 `signing/debug.keystore`，下面三个参数**永久不变**，直接填写即可：

| 字段 | 填写值 |
| --- | --- |
| 服务平台 | Android 平台 |
| PackageName（包名） | `com.example.nearbydiscount.debug` |
| 调试版安全码 SHA1 | `E9:9D:A9:36:17:16:CE:04:E6:DC:0C:37:D0:06:2D:46:DE:5B:5D:66` |
| 发布版安全码 SHA1 | 同上（当前仅有 debug 包；正式发布换成 release 证书后需重新申请） |

步骤：控制台 → 应用管理 → 创建新应用 → 添加 Key → 服务平台选「Android 平台」→ 填入上表包名与 SHA1 → 提交，复制生成的 Key。

拿到 Key 后二选一：
- **云端出包**：在仓库 **Settings → Secrets and variables → Actions** 添加 Secret `AMAP_KEY`，值为刚才的 Key，再重新运行工作流。
- **本地构建**：填入 `local.properties` 的 `amap.key`。

> 包名务必是带 `.debug` 后缀的那个（debug 构建类型设置了 `applicationIdSuffix=".debug"`），填错会报 `INVALID_USER_SCODE`。签名固定在 `signing/debug.keystore`（公开调试证书，密码 `android`），所以本地与 CI 构建出的包 SHA1 一致，同一把 Key 两边通用。

## 云端构建 APK（无需本地安装任何工具）

工程已包含 `.github/workflows/build-apk.yml`，用 GitHub Actions 自动构建：

1. 在 GitHub 新建一个仓库（例如 `nearby-discount`）。
2. 在本工程目录执行（把地址换成你的仓库）：

   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/<你的用户名>/nearby-discount.git
   git branch -M main
   git push -u origin main
   ```

   `local.properties` 已被 `.gitignore` 排除，密钥不会被上传。
3. 打开仓库的 **Actions** 页面，等待 `Build Android APK` 运行完成（首次约 5–10 分钟）。
4. 进入该次运行，在页面底部 **Artifacts** 下载 `nearby-discount-debug-apk`，解压得到 `app-debug.apk`。
5. 把 APK 传到安卓手机，允许「安装未知来源应用」后安装即可。

常见问题：
- 想重新构建：进入 Actions → `Build Android APK` → **Run workflow**。
- 需要把 API Key 打进包里：在仓库 **Settings → Secrets and variables → Actions** 添加 `MEITUAN_LBS_KEY`（和可选的 `DEALS_BRIDGE_URL`、`AMAP_KEY`），再重新运行工作流。
- 想用地图选点：添加 Secret `AMAP_KEY`（高德 Android Key）。不配也能构建，但选点页会提示未配置。
- 没配置 Secrets 也能构建成功，但 App 启动时会提示「尚未配置官方 Key」并拒绝请求；这是刻意的合规拦截，不是构建失败。
- debug APK 使用仓库内固定的调试签名（`signing/debug.keystore`，SHA1 见上节），确保高德 Key 绑定长期有效；正式分发需配置 release 签名后改用 `:app:assembleRelease`。

## 快速开始

1. 安装 Android Studio（Ladybug 或更新）、JDK 17、Android SDK 35。
2. 复制配置模板：`copy local.properties.example local.properties`。
3. 填写：
   - `meituan.lbs.key`：在美团地图开放平台申请 **Web服务API 类型 Key**。
   - `deals.bridge.url`：你的服务端地址（HTTPS），由服务端持有美团授权凭证调用官方优惠接口。留空则优惠列表为空，但商家检索正常。
   - `amap.key`：在高德地图开放平台创建 Android 应用并添加 Key，用于「选择位置」地图选点。包名与 SHA1 用文末「申请高德 Key」一节的固定值即可。留空则选点功能不可用，GPS 检索正常。
   - 本地调试可临时设 `enable.demo.data=true`（仅 debug 包生效）以查看过滤效果。
4. Android Studio 打开 `NearbyDiscount/` 目录，Sync + Run。
5. 命令行：`./gradlew :app:assembleDebug`；单测：`./gradlew :app:testDebugUnitTest`。

> 若首次打开提示缺少 `gradle-wrapper.jar`，在 Android Studio 中执行一次 Gradle Sync 会自动补齐，或本机执行 `gradle wrapper --gradle-version 8.9` 生成。

## 优惠数据的接入说明

美团没有面向任意第三方 App 的「公开消费端附近优惠」接口。因此本工程采用合规架构：

```
Android App --HTTPS--> 你的服务端 --官方授权 API--> 美团
   (不持有密钥)          (持有授权凭证)         (数据来源)
```

桥接接口约定：`GET {deals.bridge.url}promotions?poiIds=id1,id2`，返回

```json
{
  "code": 0,
  "message": "",
  "serverTime": 1700000000000,
  "promotions": [
    {
      "id": "act-1", "poiId": "商家ID", "title": "到店立减20元",
      "discountText": "8.5折", "status": "ACTIVE", "enabled": true,
      "startAt": "2026-11-01T00:00:00Z", "endAt": "2026-12-31T23:59:59Z",
      "stock": 100, "claimed": 30, "url": "https://..."
    }
  ]
}
```

字段命名可适配，只需在 `PromotionDto` 中调整。有效状态白名单与过滤规则集中在 `DiscountValidator`。

## 发布前建议

- 在 `app/src/release/res/xml/network_security_config.xml` 为 `lbsapi.meituan.com` 和桥接域名增加证书锁定：

  ```xml
  <domain includeSubdomains="true">lbsapi.meituan.com</domain>
  <pin-set expiration="2027-01-01">
      <pin digest="SHA-256">替换为真实证书公钥哈希</pin>
  </pin-set>
  ```

  哈希必须来自官方证书，写错会导致正式包全部请求失败。
- 配置 release 签名（见 `local.properties.example`），开启 R8 后验证序列化类未被裁剪。
- 补充隐私政策与位置用途说明，遵循最小必要原则；上架时按平台要求申报权限。
- 遵守调用频率限制（例如外卖折扣类接口按 app 维度限流），在服务端做缓存与限流，不要高频轮询。

## 已知限制

- 未配置桥接服务时不展示优惠，这是刻意的安全降级，不会改用抓取等非官方手段。
- 美团 LBS 使用 GCJ-02 坐标，设备定位可能存在坐标系偏差；优先使用接口返回的 `distance` 字段。
- 接口返回结构在不同版本间可能包装层级不同，`PoiEnvelope` 已兼容 `entity` / `poi` / 平铺三种形式。
