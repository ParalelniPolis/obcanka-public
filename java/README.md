
Java a Android knihovna pracující s eObčankou
====================================
Doporučená četba: [zde](https://github.com/ParalelniPolis/obcanka-public/blob/master/java/doc/Obcanka_20190119.pdf)

Požadavky
=========
1. Podporovaný hardware pro Linux nebo Windows:
- USB Reader HID Omnikey 3021 
- [Čtečka čipových karet +iD](https://www.xtel.cz/obchod/usb-ctecka-cipovych-karet-id)
2. Podporovaný hardware pro Android
- ACS ACR38U-ND PocketMate II
- ACS ACR39U-NF PocketMate II USB-C Smart Card Reader
- ACS ACR39U-N1 PocketMate II
3. Požadavky na software
- Projekt je vyvíjen na Linuxu (Ubuntu 16.04), ale fungovat by měl i na ostatních Linux distribucích a Windows.
- Je požadováno Open JDK 8 (Pozor s Oracle JDK nefunguje detekce čtečky karet). S vyšší verzí JDK nelze kompilovat Android část.
- Pokud chcete kompilovat také Android část odkomentujte Android moduly v settings.gradle

Instrukce pro vývojáře
=========
- Projekt si nejdříve forkněte a své změny do hhlavního projektu zasílejte prostřednictvím pull requestů

Postup
=========
0. Stažení projektu
```bash
 git clone https://github.com/ParalelniPolis/obcanka-public.git
```
1. Kompilace:
```bash
 ./gradlew clean build
```
2. Stažení issuer certifikátů z bitcoin blockchainu.
```bash
 ./gradlew download
```
Certifikát bude uložen do ./public_tools/issuer_of_short.crt
3. Spuštění příkladu, který přečte certifikáty z eObčanky a uloží je na disk. **Vyžaduje čtečku karet**.
```bash
 ./gradlew helloworld
```
Certifikáty budou uloženy do ./desktop_app/short.crt a ./desktop_app/long.crt
4. Ověření zda byl certifikát stažený z karty vydán issuerem jehož certifikát byl stažen z blockchainu
```bash
 ./gradlew verify
```
5. Ověření správnosti IOK přihlášením na portál občana. **Vyžaduje čtečku karet**
```bash
 ./gradlew authenticate
```
