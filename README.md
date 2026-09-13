# AI Super Cleaner Ultimate

تطبيق Android لتنظيف التخزين وفحص الخصوصية وإدارة الملفات محليًا، مع إعلانات للمستخدم المجاني وميزات Premium عبر Google Play Billing.

## الهوية والمنتجات

- Application ID: `com.aisupercleaner.ultimate`
- الاشتراك الشهري: `premium_monthly` — السعر الاحتياطي `$2.99`، والسعر النهائي من Google Play.
- الشراء الدائم: `premium_lifetime` — السعر الاحتياطي `$19.99`، والسعر النهائي من Google Play.

## الصلاحيات المهمة

يستخدم التطبيق `QUERY_ALL_PACKAGES` لأن Privacy Scanner يفحص التطبيقات المثبتة والأذونات المطلوبة منها. ويستخدم `MANAGE_EXTERNAL_STORAGE` لأن وظائف إدارة وتنظيف الملفات تحتاج وصولًا واسعًا. يجب تقديم Permissions Declaration Form للصلاحيات المقيدة وإضافة إفصاح واضح داخل التطبيق ووصف المتجر.

## البناء والفحص

```bash
./gradlew :app:assembleDebug --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleRelease --no-daemon
./scripts/qa_release.sh
./scripts/verify_release.sh app/build/outputs/apk/release/app-release-unsigned.apk
```

ينتج Release APK غير موقع ما لم تُمرر إعدادات التوقيع في بيئة محمية. احفظ `mapping.txt` لكل إصدار Release.

## توقيع GitHub Actions

يُستخدم `.github/workflows/release.yml` لبناء AAB وAPK موقّعين عبر GitHub Secrets. لا ترفع Keystore أو كلمات المرور إلى المستودع. الأسرار المطلوبة هي:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

## قبل النشر

إعداد منتجات Billing في Google Play، إنشاء رسالة UMP، ربط سياسة الخصوصية، تعبئة Data Safety، تقديم نماذج الصلاحيات المقيدة، إضافة License Testers، ثم اختبار AAB عبر Internal Testing.
