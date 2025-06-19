# Настройка iOs

Актуальная версия на данный момент - iPhone 14, iOS 16.4 (на более новых проблемы взаимодействия с appium)

## Установка Appium
Установить appium (через homebrew), затем выполнить \
`appium driver install xcuitest` \
(https://github.com/appium/appium-xcuitest-driver)

## Настройка XCode

Установить актуальную версию xcode, переместить приложение в /Applications.
Выполнить

`«sudo xcode-select --print-path`

если результат отличен от /Applications, выполнить

`sudo xcode-select -s /Applications/Xcode.app` \
`sudo xcode-select --install`

(https://solutionfall.com/question/why-do-i-encounter-an-xcrun-iphonesimulator-error-when-running-the-toolchain-build-for-kivy/)

Для проверки можно вызвать \
`xcrun --sdk iphonesimulator --show-sdk-version` \
должен вернуть номер версии iOs

## Настройки эмулятора
### Установка WDA
Исходник:
https://github.com/appium/WebDriverAgent \
Инструкция:
https://appium.github.io/appium-xcuitest-driver/7.22/guides/wda-custom-server/

Сейчас установленный WDA требуется, за это отвечает параметр в capabilites \
`appium:usePreinstalledWDA`

### Отключение запросов местоположения в Safari
Settings -> Safari -> Location -> Deny

### Важно

При старте агентов браузер эмулятора должен находиться в одном из двух состояний:
- запущен в первый раз (открыта одна вкладка в обычном сеансе)
- открыта одна приватная вкладка (количество неприватных вкладок при этом вроде как не важно)
в противном случае нарушается процесс переоткрытия вкладки

## Capabilites для ручного подключения в Appium Inspector
Если понадобится определить пути к элементам браузера: \
`{
"platformName": "iOS",
"appium:platformVersion": "17.5",
"appium:automationName": "XCUITest",
"browserName": "Safari",
"appium:usePreinstalledWDA": true,
"appium:wdaStartupRetryInterval": 20000,
"webviewConnectTimeout": 30000
}`

(//XCUIElementTypeButton[@name="Close"])[2]

