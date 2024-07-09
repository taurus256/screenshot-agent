# Настройка iOs

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

## Установка WDA на эмулятор
Исходник:
https://github.com/appium/WebDriverAgent \
Инструкция:
https://appium.github.io/appium-xcuitest-driver/7.22/guides/wda-custom-server/

Сейчас установленный WDA требуется, за это отвечает параметр в capabilites \
`appium:usePreinstalledWDA`

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
