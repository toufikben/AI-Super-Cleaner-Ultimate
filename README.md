# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–11 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، ضغط النسخ وسجلها، Quick Clean وAdvanced Smart Scan، Cache، bounded concurrency، الاختبارات، AdMob، وRewarded Ads الاختيارية.

**المرحلة 12 مكتملة:** Google Play Billing، Premium entitlement، اشتراك شهري، Lifetime، وPaywall واضح.

## Premium

يستخدم المشروع المنتجين:

- `premium_monthly` للاشتراك الشهري.
- `premium_lifetime` للشراء الدائم.

يتحقق BillingManager من ProductDetails ومشتريات Google Play، ويعتمد Premium فقط عند Purchase بحالة PURCHASED مع acknowledgement. يعرض Paywall إزالة الإعلانات وAdvanced AI Scan وSimilar Photos وVideo compression وScheduled Scan وStorage history. تظل Free cleaning متاحة، ولا يكون Premium أو الإعلان شرطاً لحذف ملف.

قبل الإصدار يجب إنشاء المنتجات في Play Console، اختبار License Testers، إضافة استعادة مشتريات ومزامنة خلفية مناسبة، ومراجعة Terms وPrivacy.

## الإعلانات

Rewarded Ads اختيارية فقط، والإعلانات البينية لها cooldown. لا يظهر إعلان داخل اختيار الملفات أو تأكيد Trash أو الحذف الدائم.

## الخصوصية

تظل الملفات على الجهاز. لا يستخدم التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يتجاوز قيود Android.

## البناء

```bash
./gradlew assembleDebug lintDebug --no-daemon
```

## خارطة التنفيذ التالية

1. Google UMP والموافقة والـPrivacy/Data Safety.
2. استعادة المشتريات وLicense Testing وInstrumentation.
3. اختبارات الإصدار النهائية والنشر التجريبي.
