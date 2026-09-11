# AdMob Production Setup

تم إنشاء تطبيق Android داخل حساب AdMob باسم **AI Super Cleaner Ultimate**.

## Production IDs

| الاستخدام | ID |
|---|---|
| AdMob Application ID | `ca-app-pub-2934454612171100~4587152199` |
| Interstitial Cleanup | `ca-app-pub-2934454612171100/8721838321` |
| Rewarded Advanced Scan | `ca-app-pub-2934454612171100/7133782308` |

تم ربط Application ID في `app/src/main/AndroidManifest.xml`، وربط وحدتي الإعلان في `AdManager.kt`.

## سياسة الاستخدام داخل التطبيق

- Interstitial لا يظهر بعد كل إجراء، ويخضع لـ cooldown.
- Rewarded اختياري لفتح Advanced Scan مرة واحدة.
- لا يُطلب إعلان لحذف ملف.
- Premium يعطل الإعلانات.
- UMP Consent يجب أن يكتمل قبل تهيئة AdMob.

## حالة حساب AdMob

عند إعداد التطبيق ظهر أن الحساب والتطبيق قيد المراجعة من AdMob. قد يستغرق التحقق عادةً 24 ساعة أو أكثر. لن نعتبر الإعلانات جاهزة للإنتاج حتى تكتمل المراجعة ويظهر التطبيق كمعتمد.

## ما تبقى في AdMob

1. إنشاء UMP Privacy & messaging message.
2. ربط Privacy Policy URL العام.
3. مراجعة إعدادات الخصوصية والمناطق.
4. انتظار مراجعة الحساب والتطبيق.
5. اختبار الوحدات على APK/AAB موقع.
6. مراجعة Policy Center قبل Internal Testing.

## التحقق البرمجي

تم تشغيل QA بدون السماح بـTest IDs:

```bash
./scripts/qa_release.sh
```

والنتيجة `QA checks passed` و`BUILD SUCCESSFUL`.
