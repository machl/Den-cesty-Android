Den cesty mobilní aplikace pro Android zařízení 
===

Instalace vývojového prostředí
---

Nainstalujte si oficiální vývojové prostředí [Android Studio](https://developer.android.com/sdk/index.html).

Dále prostřednictvím Android Studia a jeho SDK Manageru nainstalujte následující: 

* Android SDK Tools
* Android SDK Platform-tools
* Android SDK Build-tools
* Android 4.1.2 (API 16) nebo novější (nejlépe úplně nejnovější):
  * Documentation for Android SDK
  * SDK Platform
  * Google APIs
* Extras
  * Android Support Repository
  * Android Support Library
  * Google Play services
  * Google Repository


Překlad projektu
---

Otevřete tuto složku jako projekt v Android Studiu. Zobrazí se Vám už přednastavený projekt s mobilní aplikací.

Tlačítkem `Build -> Make Projekt` přeložíte zdrojové kódy do mobilní aplikace.

Spuštění aplikace
---

Přeloženou aplikaci spustíte na připojeném zařízení nebo v emulátoru volbou `Run -> Run 'app'`.

Pokud hodláte spustit aplikaci v emulátoru, musíte mít přes SDK Manager navíc staženo:

* Google APIs Intel x86 Atom_64 System Image v API 19 nebo novějším

a následně vytvořit skrz AVD Manager virtuální stroj právě s Google APIs image.

Simulovat polohu zařízení lze pouze v emulátoru a to následujícím postupem:

> V termínálu zadejte příkaz
> 
>```telnet localhost 5554```
>
> a následně příkazem
> 
> ```geo fix <longitude value> <latitude value>```
> 
> pošlete informaci o poloze do emulátoru.

Konfigurace aplikace
---

**Změna adresy serveru**

Změnu serveru, se kterým aplikace komunikuje, provedete změnou adresy na řádce
 
```java
public static final String URL_WEBSERVER = "https://www.dencesty.cz";
```
 	
v souboru `WebAPI.java`.

**Změna parametrů snímání polohy**

Parametry snímání polohy lze upravit v souboru `BackgroundLocationService.java`. Základní hodnoty jsou:

```java
public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 60 * 1000;
public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
public static final int LOCATION_UPDATES_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
```
