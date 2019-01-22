
Seznam projektů pracujících s eObčankou
=========================================
| Jmeno projeku | Popis projektu | Odkaz na projekt|
| --- | --- | --- |
| **obcanka-reader**|Čtečka eObcanek jako USB klavesnice | https://github.com/premek/obcanka-reader |
| **Czech ID card service** |Multiplatformni open-source identifikacni klient  | https://github.com/devnautica/czeidcardservice |
| **soFair-eObcanka** | JS knihovna pro přihlášení, postaveno nad řešením Czech ID card service + použití v aplikaci petiční systém | https://github.com/Hixqqo/soFair-eObcanka |
| **eObcanka.Java** |  Java a Android knihovna pro práci s eObčankou  | https://github.com/ParalelniPolis/obcanka-public/tree/master/java| 
| **eObcanka.NET** |  .NET knihovna pro práci s eObčankou  | https://github.com/wurducius/eObcanka.NET | 
| **oPYcanka** | Python knihovna pro práci s eObčankou | https://github.com/ParalelniPolis/opycanka |
| **eobcanka-rust** | Minimální Rust lib impementace pro práci s eObčankou | https://github.com/Qubeo/eobcanka-rust |
| **EOP** | Python skript umožnující výpočet veřejného klíče z podpisu certifikátu uloženého na eObčance  | https://github.com/andrewkozlik/eop

Zajímavé dokumenty k eObčance
========================================
| Popis | Odkaz |
| --- | --- |
| Prezentace pro vývojáře z Hackathonu k základnímu seznámení se s eObčankou | https://github.com/ParalelniPolis/obcanka-public/blob/master/java/doc/Obcanka_20190119.pdf |
| ISO7816 | http://cardwerk.com/smart-card-standard-iso7816-4-section-5-basic-organizations |

Neveřejné veřejné
==================
**Česká republika zavádí nové elektronické průkazy (tzv. eObčanka) a celý projekt působí velmi rozpačitě. Technické řešení i komunikace snižují důvěryhodnost nového nástroje, který má přitom potenciál významně zjednodušit komunikaci se státem a ulehčit řadu povinností identifikace uživatelů internetových a finančních služeb. Namísto otevření software pro další vývoj na straně uživatelů však stát odmítá zveřejnit i údaje, které by ze své podstaty veřejné být měly. Rozhodli jsme se proto otevřít toto neveřejné veřejné paralelně.**

Architektura řešení je hrubě nedotažená a postrádá logickou strukturu, ale i některé základní bezpečnostní náležitosti. Při rozkrývání technického provedení se pak objevil zcela nepochopitelný problém na straně vydavatele, který odmítl zveřejnit veřejné klíče pro ověření autenticity certifikátů a jejich vydavatele (certifikační autority).

S požadavkem na zpřístupnění veřejných klíčů se na Ministerstvo vnitra ČR obrátil v polovině září tohoto roku Karel Kyovský. „Zpřístupnění uvedených certifikátů široké veřejnosti umožní občanům i soukromému sektoru efektivnější využití vlastností nových občanských průkazů a umožní nezávislé ověření pravosti“ uvedl Karel Kyovský v odůvodnění žádosti o zpřístupnění veřejných klíčů.

Ministerstvo vnitra však žádost zamítlo s odkazem na sérii předpisů, které údajně znemožňují veřejné klíče poskytnout, a samotné zveřejnění označilo za bezpečnostní riziko. „Poskytnutí požadovaných certifikátů považujeme za možné riziko pro bezpečnost systému elektronické komunikace,“ uvedl Ing. František Varmuža, ředitel Odboru centrálních informačních systémů Ministerstva vnitra ve své odpovědi.

*   [Stát odmítá vydat veřejné klíče k elektronickým občanským průkazům](https://www.lupa.cz/aktuality/stat-odmita-vydat-verejne-klice-k-elektronickym-obcanskym-prukazum/)
*   [Ministerstvo vnitra ČR považuje vydání veřejných klíčů k občankám za bezpečnostní riziko](https://www.root.cz/zpravicky/ministerstvo-vnitra-cr-povazuje-vydani-verejnych-klicu-k-obcankam-za-bezpecnostni-riziko/)

Ministerstvo vnitra buď nerozumí základům kryptografie a bezpečnosti, nebo jde o skrytý záměr, jehož cílem je vytvoření role centrálního ověřovatele (prostor pro další zakázky). Veřejné klíče k e-dokladům přitom bez problémů zpřístupnilo například Slovensko nebo Estonsko, kde žádné bezpečnostní riziko neidentifikovali.

**Veřejné klíče české eObčanky lze ovšem z certifikátů nahraných na eObčance dopočítat.** Metodiku výpočtu představili konzultanti v oblasti kryptografie Ondřej Vejpustek a Andrew Kozlik. Bližší metodologie je popsaná například v [dokumentu Elliptic Curve Cryptography](http://bitcoin.me/sec1-v2.pdf).

Veřejný klíč je [zapsán v blockchainu](https://www.blockchain.com/btc/tx/a0549be380a0eb8d623c9e18a072e494952333a96921db393dbb4c5cfddea86c). Jak klíč stáhnout [naleznete zde](https://github.com/ParalelniPolis/obcanka-public).

Odmítnutím zveřejnit veřejné klíče k certifikátům stát vytváří umělou monopolní situaci a prostor pro korupci, kdy pouze jím určené osoby s přístupem k certifikátům mohou využít nové vlastnosti občanských průkazů s čipem a vytvářet další aplikace pro širší a smysluplnější využití eObčanky.

Pokud stát nesmyslně odmítá veřejnosti veřejností financovaný systém otevřít, učiníme to za něj (není zač).

![Hackathon](https://github.com/b00lean/obcanka-public/raw/master/img/Hackaton.jpg)

### Hackathon eObčanka 2019 19.-20. 1. 2019 v Paralelní Polis
Třetí lednový víkend pořádá Paralelní Polis hackathon, který si klade za cíl otevřít platformu eObčanek, zkontrolovat jejich bezpečnost a přinést nové možnosti jejich využití.

#### Podmínky

Hackathonu se mohou zúčastnit skupiny i jednotlivci. Na akci se nehradí žádné startovné. K dispozici budou čtečky karet a příkladová JAVA knihovna pro komunikaci s eObčankou. eObčanku si musí účastníci zajistit sami. Po dobu hackathonu bude k dispozici občerstvení a výběrová káva z naší kavárny.

#### Cíle

Během hackathonu chceme vytvořit projekty v následujících oblastech:

*   Knihovny pro obsluhu eObčanky pro různé platformy
*   Aplikace pro iOS
*   Extrakce software z karty, bezpečnostní audit - například z hlediska manipulace s privátním klíčem
*   Prozkoumání a zdokumentování nezveřejněných funkcí systému
*   Nová využití eObčanky pro komerční i nekomerční účely

Účastníci se nemusí omezovat pouze na tato témata a mohou přijít s vlastními zajímavými nápady. Kreativitě se meze nekladou.

### Kritéria hodnocení

Projekty budou hodnoceny na základě pětiminutové prezentace a dema dle následujících kritérií:

*   Užitečnost
*   UX
*   Originalita
*   Kvalita dema

### Ceny

Vítězné týmy se mohou těšit na:

*   Nabídku práce na ministerstvu vnitra ČR
*   DVD s filmem Občan K
*   Podíl na [bitcoinové odměně 3Bz3pGkTQJf7NSxhtE8YicHffyDRCbjqeb](https://www.blockchain.com/btc/address/3Bz3pGkTQJf7NSxhtE8YicHffyDRCbjqeb)

Nemůžete se zúčastnit, ale chcete týmy motivovat? Přispějte na výše uvedenou bitcoinovou adresu.

### Výsledky

| Umístění | Tým | Popis projektu | Odkaz na projekt|
| --- | --- | --- | --- |
| 1. | Devnautica + soFair | Multiplatformni open-source identifikacni klient + JS knihovna pro přihlášení, postaveno nad řešením týmu Devnautica + použití v aplikaci petiční systém | https://github.com/Hixqqo/soFair-eObcanka a https://github.com/devnautica/czeidcardservice|
| 2. | Premek | Čtečka eObcanek jako USB klavesnice | https://github.com/premek/obcanka-reader |
| 3. | Auxilium & Warden Audit|	Bezpečnostní audit systému eObčanka Identifikace. Identifikace nezdokumentovaných a nezmámých funkcionalit a případných bezpečnostních chyb | TAJNÉ |
| 4. | Submission | .NET Library + Utility application | https://github.com/wurducius/eObcanka.NET | 
| 5. | oPYcanka | Python knihovna pro práci s eObčankou | https://github.com/ParalelniPolis/opycanka |
| 6. | MindFoc | Minimální Rust lib impementace | https://github.com/Qubeo/eobcanka-rust |




