# open-prices-il

אפליקציית Android להשוואת מחירים בסופרמרקטים, מבוססת על נתוני חוק שקיפות המחירים הציבוריים.

## מבנה

- `pipeline/` – איסוף ועיבוד הנתונים (רץ ב-GitHub Actions)
- `probe/` – תוצאות בדיקת הנגישות של אתרי הרשתות
- `app/` – אפליקציית Android (Kotlin + Jetpack Compose) – בהמשך

## שלבים

1. **בדיקת היתכנות** – אילו רשתות נגישות מ-GitHub Actions, וכמה נתונים כל אחת מפרסמת
2. **פייפליין** – הורדה, פענוח ובניית קובץ SQLite לכל עיר, שמתפרסם ב-Releases
3. **אפליקציה** – חיפוש מקומי בקובץ של העיר, השוואת מחירים בין סניפים, רשימת קניות

הנתונים נאספים בעזרת [il-supermarket-scraper](https://github.com/OpenIsraeliSupermarkets/israeli-supermarket-scarpers).
