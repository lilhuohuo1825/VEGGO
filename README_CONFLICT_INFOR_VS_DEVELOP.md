# Huong dan resolve conflict: `infor` so voi `develop`

Tai lieu nay tom tat thay doi tren nhanh `infor` so voi `develop` hien tai, de khi merge/rebase gap conflict co the biet nen giu phan nao.

## Tong quan

- Nhanh hien tai: `infor`
- Base doi chieu: `develop`
- Commit tren `infor`: `5113a75 thanhthao`
- Pham vi thay doi chinh: du lieu Community Cooking, Room database schema/version, UI man hinh Community, Policies va Support Customers.

## Nguyen tac chon khi conflict

Neu conflict lien quan den tinh nang community cooking/recipe ingredient, uu tien lay ban cua `infor` va giu dong bo cac file sau:

- `app/src/main/assets/community_cooking.json`
- `app/src/main/java/com/veggo/app/data/local/entity/CommunityRecipeIngredientEntity.java`
- `app/src/main/java/com/veggo/app/core/database/DatabaseManager.java`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/31.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/32.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/33.json`

Ly do: `community_cooking.json` da them field `productSku` cho ingredients, thay doi mapping san pham that, iconUrl va quantity. Entity Room cung da them cot `productSku`, va database version da tang len `33`. Neu chi lay JSON ma khong lay entity/schema/version thi app de loi schema hoac seed/read data khong dung.

## Chi tiet thay doi can giu

### 1. Du lieu community cooking

File: `app/src/main/assets/community_cooking.json`

- Cap nhat dataset community cooking lon:
  - `categories`: 11
  - `chefs`: 24
  - `recipes`: 144
  - `recipeIngredients`: 864
  - `recipeGalleries`: 432
- Nhieu recipe duoc dua `ingredientCount` ve `6`.
- Ingredients duoc map sang product that, co them:
  - `productSku`
  - `iconUrl`
  - `displayName`
  - `quantity`
- Nhieu URL anh category/chef/recipe duoc thay doi.

Huong resolve:

- Neu conflict chi o data community cooking, uu tien lay `infor`.
- Neu `develop` co them recipe/category moi, merge tay vao cau truc cua `infor`, nhung moi ingredient moi nen co du cac field `productId`, `productSku`, `displayName`, `iconUrl`, `iconEmoji`, `quantity`, `sortOrder`.
- Sau khi resolve, kiem tra JSON parse duoc.

### 2. Room database/schema

Files:

- `app/src/main/java/com/veggo/app/core/database/DatabaseManager.java`
- `app/src/main/java/com/veggo/app/data/local/entity/CommunityRecipeIngredientEntity.java`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/31.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/32.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/33.json`

Thay doi:

- `DATABASE_VERSION` tang tu `30` len `33`.
- `CommunityRecipeIngredientEntity` them field/getter/setter/constructor param `productSku`.
- Them schema export version `31`, `32`, `33`.

Huong resolve:

- Uu tien giu version database cao nhat. Neu `develop` sau nay da tang len tren `33`, khong ha version xuong; can merge them cot `productSku` vao schema/version moi.
- Khong bo `productSku` khoi entity neu van giu `community_cooking.json` cua `infor`.
- Giu cac file schema moi neu Room dang bat export schema.

### 3. Community UI va navigation

Files:

- `app/src/main/java/com/veggo/app/presentation/community/CommunityDiscoveryActivity.java`
- `app/src/main/java/com/veggo/app/presentation/community/CommunityIngredientsActivity.java`
- `app/src/main/java/com/veggo/app/presentation/community/CommunityUi.java`
- `app/src/main/res/layout/activity_community_*.xml`
- `app/src/main/res/layout/item_community_chip.xml`

Thay doi:

- Discovery screen them nut back `discoveryBackButton` va listener `finish()`.
- Header search trong `CommunityUi.setupTopHeader()` mo `CommunityDiscoveryActivity`.
- Inflate chip dung parent de layout params dung hon.
- Ingredients screen:
  - checkbox mac dinh la chua chon (`false`) thay vi pattern theo index.
  - bo tham so `index` khoi `bindIngredient`.
- Dieu chinh mau icon ve `#57AF37` o nhieu man hinh community.
- `activity_community_discovery.xml` doi layout search bar, them back button, dung `bg_home_search`/`bg_home_mic`, can chinh sizing.
- `item_community_chip.xml` giam height tu `58dp` xuong `50dp`, margin end tu `14dp` len `16dp`.

Huong resolve:

- Neu conflict o search/back/header community, uu tien `infor` vi code Java va XML dang phu thuoc nhau, dac biet `discoveryBackButton`.
- Neu `develop` co them UI element moi, merge vao layout cua `infor` nhung giu cac id da duoc Java dung.
- Khong xoa `communitySearchButton` behavior trong `CommunityUi.setupTopHeader()` neu muon search button mo discovery.

### 4. Policies va Support Customers UI

Files:

- `app/src/main/res/layout/activity_policies.xml`
- `app/src/main/res/layout/activity_support_customers.xml`

Thay doi:

- Header back/notify chuyen tu `ImageButton` sang `FrameLayout` + `AppCompatImageView`.
- Them/duy tri stroke transparent cho mot so `MaterialCardView` de tranh vien mac dinh.
- Can chinh mau icon, card va mot so spacing.

Huong resolve:

- Neu conflict chi la UI style, uu tien `infor` de giu giao dien da can chinh.
- Neu `develop` co thay doi logic id hoac id duoc Java tham chieu, phai giu id do va chi merge style cua `infor`.

## File nen uu tien lay `infor`

- `app/src/main/assets/community_cooking.json`
- `app/src/main/java/com/veggo/app/data/local/entity/CommunityRecipeIngredientEntity.java`
- `app/src/main/java/com/veggo/app/core/database/DatabaseManager.java`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/31.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/32.json`
- `app/schemas/com.veggo.app.core.database.VeggoDatabase/33.json`
- `app/src/main/java/com/veggo/app/presentation/community/CommunityDiscoveryActivity.java`
- `app/src/main/java/com/veggo/app/presentation/community/CommunityIngredientsActivity.java`
- `app/src/main/java/com/veggo/app/presentation/community/CommunityUi.java`
- `app/src/main/res/layout/activity_community_discovery.xml`
- `app/src/main/res/layout/activity_community_ingredients.xml`
- `app/src/main/res/layout/item_community_chip.xml`

## File co the merge tay

- Cac layout community chi doi tint/mau icon:
  - `activity_community_categories.xml`
  - `activity_community_chefs.xml`
  - `activity_community_cookbook.xml`
  - `activity_community_follow_list.xml`
  - `activity_community_home.xml`
  - `activity_community_post.xml`
  - `activity_community_profile.xml`
  - `activity_community_recipe_detail.xml`
  - `activity_community_recipes.xml`
- `activity_policies.xml`
- `activity_support_customers.xml`

Voi nhom nay, neu `develop` co update noi dung/hoc logic UI moi, merge tay va giu style/id can thiet cua `infor`.

## Checklist sau khi resolve conflict

- JSON parse thanh cong:
  - `Get-Content -Raw app/src/main/assets/community_cooking.json | ConvertFrom-Json | Out-Null`
- Kiem tra entity ingredient van co `productSku`.
- Kiem tra `DATABASE_VERSION` khong bi giam xuong duoi version schema moi nhat.
- Build app sau merge:
  - `./gradlew :app:assembleDebug`
- Mo nhanh cac man hinh:
  - Community Home
  - Community Discovery
  - Recipe Detail
  - Ingredients
  - Policies
  - Support Customers
