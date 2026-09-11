# Phase 11 Completion Record

## Scope

تم دمج AdMob خلف مدير مستقل مع Rewarded Ad اختياري، cooldown للإعلانات البينية، وفشل آمن لا يعطل التطبيق.

## Delivered

أضيف `AdManager` لتهيئة Mobile Ads وتحميل Rewarded مسبقاً. يستخدم Rewarded Ad اختبارياً لفتح جلسة Advanced Smart Scan واحدة فقط بعد callback مكافأة ناجح. إذا لم يكن الإعلان جاهزاً أو فشل التحميل تظهر رسالة مفهومة، ويبقى Advanced Smart Scan متاحاً دون إجبار المستخدم على الإعلان.

أضيف interstitial policy بمدة cooldown مقدارها 15 دقيقة مع timestamp محلي. لا يتم استدعاؤها من مسار الحذف، ويمكن استخدامها لاحقاً فقط بعد اكتمال عملية آمنة وفي نقطة انتقال طبيعية.

أضيفت واجهة اختيارية داخل Clean: Watch a short ad to unlock one Advanced Scan. زر Advanced Smart Scan الأساسي لا يعتمد على الإعلان. لا يظهر إعلان قبل اختيار الملفات أو أثناء شاشة التأكيد أو قبل Move to Trash أو Delete Permanently.

تم استخدام Google test App ID وtest ad unit IDs حالياً حتى لا تُستخدم حسابات إنتاجية أثناء التطوير.

## Verification

تم تشغيل:

```bash
./gradlew assembleDebug lintDebug --no-daemon
```

والنتيجة `BUILD SUCCESSFUL` مع نجاح البناء وlint.

## Acceptance

- Rewarded اختياري وليس شرطاً للحذف.
- Reward لا يُمنح قبل callback نجاح المكافأة.
- فشل الإعلان لا يمنع الوظائف الأساسية.
- Interstitial cooldown محلي ومحدد.
- لا إعلان بعد كل scan أو كل إجراء.
- لا إعلان داخل تأكيد الحذف أو Trash.
- Test IDs واضحة ومحصورة في مدير الإعلان.

## Release requirements

قبل النشر يجب استبدال Test App ID وTest Ad Unit IDs بمعرفات إنتاجية، إضافة Google UMP/consent flow للمناطق المطلوبة، مراجعة Privacy Policy وData Safety، وضبط frequency caps من AdMob console. لا يجوز نشر Test IDs كإعداد إنتاجي.
