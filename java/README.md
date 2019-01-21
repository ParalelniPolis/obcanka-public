
Java knihovna pracující s eObčankou
====================================

Požadavky
=========
1. Podporovaný hardware pro Linux nebo Windows:
- USB Reader HID Omnikey 3021 
- [Čtečka čipových karet +iD][https://www.xtel.cz/obchod/usb-ctecka-cipovych-karet-id]  
2. Podporovaný hardware pro Android
- ACS ACR38U-ND PocketMate II
- ACS ACR39U-NF PocketMate II USB-C Smart Card Reader
- ACS ACR39U-N1 PocketMate II
3. Požadavky na software
- Projekt je vyvíjen na Linuxu (Ubuntu 16.04), ale fungovat by měl i na ostatních Linux distribucích a Windows.
- Je požadováno Open JDK 8 (Pozor s Oracle JDK nefunguje detekce čtečky karet). S vyšší verzí JDK nelze kompilovat Android část.
4. Kompilace:
```bash
 ./gradlew clean build
```
5. Stažení issuer certifikátů z bitcoin blockchainu.
```bash
 ./gradlew download
```
Certifikát bude uložen do ./public_tools/issuer_of_short.crt
6. Spuštění příkladu, který přečte certifikáty z eObčanky a uloží je na disk. *Vyžaduje čtečku karet*.
```bash
 ./gradlew helloworld
```
Certifikáty budou uloženy do ./desktop_app/short.crt a ./desktop_app/long.crt
7. Ověření zda byl certifikát stažený z karty vydán issuerem jehož certifikát byl stažen z blockchainu
```bash
 ./gradlew verify
```
8. Ověření správnosti IOK přihlášením na portál občana
```bash
 ./gradlew authenticate
```
