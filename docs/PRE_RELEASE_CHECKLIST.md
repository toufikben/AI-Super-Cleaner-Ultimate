# قائمة ما قبل النشر — AI Super Cleaner Ultimate


آخر تحديث: 12 سبتمبر 2026 — المرحلة الحالية: 15 إعداد الحسابات والإصدار التجريبي


خارطة المراحل الكاملة: `docs/PROJECT_ROADMAP.md`


## الحالة الحالية


التطبيق في حالة **Release Candidate قابل للبناء**. لم يُنشر بعد على Google Play، ولم تُستخدم أسرار توقيع داخل المستودع. تم إنشاء AdMob App وProduction Ad Unit IDs في حساب AdMob.


## البنود التي سأكملها محلياً


| الحالة | البند | التحقق |
|---|---|---|
| مكتمل | Unit tests وCache وPremium وAd cooldown | `testDebugUnitTest` |
| مكتمل | Debug build وlint | `assembleDebug lintDebug` |
| مكتمل | Release build مع R8/ProGuard | `assembleRelease` |
| مكتمل | QA script وفحص الصلاحيات | `scripts/qa_release.sh` |
| مكتمل | Instrumentation test source | موجود في `app/src/androidTest` |
| مكتمل | UMP ConsentManager | يحدّث الموافقة قبل AdMob |
| مكتمل | Privacy Policy وData Safety drafts | موجودان في `docs/` |
| متبقٍ | مراجعة نتائج Instrumentation بعد تشغيلها على جهازك أو Emulator | يحتاج بيئة Android فعلية |
| متبقٍ | إصلاح أي مشكلة تظهر في اختبار الجهاز | بعد استلام النتائج |
| مكتمل | تدقيق QA بعد وضع AdMob IDs الإنتاجية | `./scripts/qa_release.sh` نجح بدون `QA_ALLOW_TEST_ADS` |


## البنود التي ستنفذها أنت على GitHub وGoogle Play


| الحالة | البند | الإجراء المطلوب |
|---|---|---|
| متبقٍ | أسرار توقيع Android | إضافة Keystore وSecrets في GitHub Actions أو بيئة إصدار محمية. لا ترفع ملف keystore إلى Git. |
| مكتمل | Production AdMob App ID وAd Unit IDs | تم إنشاء IDs واستبدالها في Manifest وAdManager. |
| متبقٍ | UMP message | إنشاء رسالة الموافقة في AdMob Privacy & messaging وربطها بالتطبيق. لوحة Privacy & messaging تحتاج متابعة من Console. |
| متبقٍ | Privacy Policy URL | نشر السياسة على رابط عام ثابت وإدخاله في AdMob وGoogle Play. |
| قيد التحقق | Billing subscription | تم إنشاء `premium_monthly`؛ يلزم تأكيد Base plan `monthly` بسعر 2.99 وتفعيله. |
| متبقٍ | Billing one-time product | إنشاء وتفعيل `premium_lifetime` بسعر 19.99 كمنتج شراء لمرة واحدة غير مستهلك. |
| متبقٍ | License testers | إضافة حسابات الاختبار وتجربة الشراء والاستعادة والإلغاء والانتهاء. |
| متبقٍ | Data Safety | إدخال الإفصاحات النهائية في Play Console بعد تثبيت إعدادات Google SDK الإنتاجية. |
| متبقٍ | Internal Testing | رفع AAB موقع إلى Google Play Internal Testing ومراجعة التقارير. |
| متبقٍ | Production rollout | لا يتم إلا بعد اجتياز Internal Testing وClosed Testing ومتطلبات Play Console. |


## أوامر التحقق المحلي


```bash
# QA التطوير مع Test Ad IDs
QA_ALLOW_TEST_ADS=1 ./scripts/qa_release.sh


# Release build غير موقع أو موقع حسب Secrets المتاحة
./gradlew assembleRelease


# عند توفر جهاز أو Emulator
./gradlew connectedDebugAndroidTest
```


## قواعد أسرار GitHub


لا تُحفظ القيم التالية داخل الملفات أو commits:


```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```


يجب حفظ كلمات المرور وBase64 للـKeystore في GitHub Actions Secrets أو بيئة إصدار محمية. ملف keystore نفسه يجب أن يكون مشفراً أو محفوظاً في Secret/Artifact آمن، وليس في المستودع العام أو الخاص.


## قاعدة Test Ads


يجب ألا تدخل Test IDs إلى نسخة Production. سكربت QA يسمح بها فقط عند استخدام `QA_ALLOW_TEST_ADS=1`. قبل إصدار Production يجب تشغيل QA من دون هذا المتغير بعد استبدال المعرفات، ثم التأكد من أن AdMob Console يعرض المعرفات الإنتاجية الصحيحة.


## ترتيب التنفيذ النهائي


1. أنت تضيف أسرار التوقيع في GitHub أو بيئة إصدار آمنة.
2. أنت تنشئ AdMob production IDs وUMP message وتجهز Privacy Policy URL.
3. أنا أستبدل نقاط الإعداد في كود الإصدار وأعيد `QA script` و`assembleRelease` و`lint`.
4. أنت تشغّل Instrumentation وBilling License Tests على جهاز أو Emulator.
5. أراجع النتائج وأصلح أي أخطاء تظهر.
6. أنت ترفع AAB إلى Google Play Internal Testing.
7. نراجع Crash reports وConsent وBilling وData Safety.
8. بعد اجتياز المراجعة فقط يتم التوسع في النشر.


## تحديث الحالة


سأذكر في كل تحديث لاحقاً:


- البنود المكتملة.
- البنود المتبقية.
- البنود التي تنتظر إجراءك.
- نتيجة آخر بناء أو اختبار.
- أي مانع يمنع الانتقال إلى Google Play.



## تحديث Billing — 12 سبتمبر 2026

الكود الحالي مطابق للمعرفات المطلوبة: `premium_monthly` للاشتراك و`premium_lifetime` للشراء لمرة واحدة، ويستخدم Billing Library 8.0.0 وSDK 36. بعد تفعيل المنتجات، يجب توليد AAB موقّع ورفعه إلى Internal Testing للتحقق من paywall.
