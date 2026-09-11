# AI Super Cleaner Ultimate

تطبيق Android حقيقي لفهم التخزين وتنظيفه بأمان، مع أولوية للخصوصية والدقة قبل تحقيق الدخل.

## حالة المشروع

**المراحل 1–12 مكتملة:** تأسيس Android وUI/UX، الخصوصية والصلاحيات، الماسح الحقيقي وRoom، Smart Cleanup Score، محركات التكرار والتشابه، التنظيف الآمن وTrash، ضغط النسخ وسجلها، Quick Clean وAdvanced Smart Scan، Cache، bounded concurrency، الاختبارات، AdMob، Rewarded Ads الاختيارية، Google Play Billing، Premium، وPaywall.

**المرحلة 13 مكتملة:** Google UMP Consent Flow، Privacy Options، Privacy Policy، وData Safety documentation.

## UMP والإعلانات

يطلب التطبيق تحديث UMP عند كل تشغيل، ويعرض Consent Form عند الحاجة، ولا يهيئ AdMob أو يطلب إعلاناً قبل اكتمال الموافقة وتحقق `canRequestAds()`. يظهر Privacy Options entry point داخل Settings عندما يطلبه UMP. فشل الموافقة لا يعطل Free cleaning، بل يبقي الإعلانات غير مهيأة.

Rewarded Ads اختيارية، والإعلانات البينية محدودة، ولا يظهر إعلان داخل حذف الملفات أو Trash.

## الخصوصية وأمان البيانات

الفهرسة والتحليل والضغط محلية. لا يرفع التطبيق الصور أو الفيديو أو الصوت أو أسماء الملفات إلى خادم التطبيق. قد تعالج Google Mobile Ads وGoogle Play بياناتها الخاصة وفق الموافقة وسياسات Google. يجب مطابقة [سياسة الخصوصية](docs/PRIVACY_POLICY.md) و[Data Safety](docs/DATA_SAFETY.md) مع إعدادات Play Console قبل الإصدار.

## البناء

```bash
./gradlew assembleDebug lintDebug --no-daemon
```

## متطلبات الإصدار التالي

1. استبدال Test Ad IDs بمعرفات إنتاجية.
2. إنشاء UMP messages وPrivacy Policy URL في AdMob.
3. استكمال Data Safety وPlay Billing disclosures.
4. اختبار الرفض والتعديل والموافقة واستعادة المشتريات على أجهزة فعلية.
