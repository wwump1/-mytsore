# Shield Sound 1.1.0 (Fabric, Minecraft 1.21.11)

Что нового:
- Звук играет и когда щит опускают ДРУГИЕ игроки рядом (затухание по расстоянию как у обычных звуков).
- Меню настроек в Mod Menu (кнопка «Настроить»): вкл/выкл, свой щит / чужие щиты,
  выбор звука из списка (◀ ▶, клик по названию — прослушать), свой ID звука, громкость 0–400%, высота тона.
- По умолчанию звук разбитого кувшина (block.decorated_pot.shatter).
- Громкость делается наложением слоёв и играет в категории MASTER, поэтому слышно
  даже при тихих «Звуках игроков» в настройках игры. Настройки: config/shieldsound.json

## Сборка
Лучший способ — положить файлы в свой рабочий проект мода (тот, из которого собирался 1.0.0):
1. Замени src/main/java/com/example/shieldsound/* и src/main/resources/fabric.mod.json файлами отсюда.
2. В build.gradle добавь:
   repositories { maven { url = 'https://maven.terraformersmc.com/releases/' } }
   dependencies { modCompileOnly "com.terraformersmc:modmenu:16.0.0" }
3. ./gradlew build  ->  build/libs/shield-sound-1.1.0.jar

Либо собери этим проектом целиком (нужен Gradle 9.2): `gradle build`.
Если Gradle не находит yarn / fabric-api / modmenu — возьми актуальные версии для 1.21.11
на https://fabricmc.net/develop и впиши в gradle.properties.

## Установка
Положи jar в mods/. Mod Menu и Fabric API должны стоять (Mod Menu — только чтобы открывать меню).
