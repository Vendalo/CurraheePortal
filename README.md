# CurraheePortal – portál skokem do survival světa

Plugin pro Paper 1.21.11, Java 21.

## Instalace
1. Sestav Mavenem (`mvn clean package`) nebo nahraj zdroje na GitHub a stáhni JAR z úspěšné akce Actions → Artifacts.
2. Zkopíruj `CurraheePortal-1.0.0.jar` do Hicoria `plugins` a restartuj.
3. Jako OP se postav na místo, kde můžeš přepsat oblast 5×5 bloků, a napiš `/portal build`.
4. Plugin vytvoří společný svět `survival` (pokud neexistuje) a postaví kamennou vodní nádrž. Hráč do ní skočí a je teleportován na spawn survival světa.
5. `/portal info` vypíše stav, `/portal remove` vypne funkci portálu (bloky zůstanou).

`plugins/CurraheePortal/config.yml` dovoluje změnit `destination.world`, nejlépe před vytvořením portálu.

**Bezpečnost:** do cizích osobních světů neteleportuje; nemá práva obcházet bezpečnostní pluginy. Nedávej obyčejným hráčům `curraheeportal.admin`.

Poznámka: `/portal build` přepisuje 5×5 bloků pod hráčem, nejprve zálohuj spawn. Zdroj nebyl otestován na spuštěném serveru.
