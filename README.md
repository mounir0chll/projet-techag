# projet tech-ag

Mini-projet Technologies des Agents : JADE, AIMA POP et planification distribuee.

## Structure

- `partie 1` : enchere JADE seller/buyers.
- `partie 2` : agent mobile JADE et decision multicritere.
- `partie 3` : test AIMA Partial Order Planner.
- `partie 4` : simulation Bill/Tom.
- `lib` : `jade.jar`.

## Execution Java

Depuis PowerShell :

```powershell
cd "C:\Users\DELL\Downloads\projet tech-ag"
javac -encoding UTF-8 -cp "lib\jade.jar" -d bin (Get-ChildItem "partie 1\src","partie 2\src" -Recurse -Filter *.java).FullName
java -cp "bin;lib\jade.jar" part1.negotiation.AuctionLauncher sold
java -cp "bin;lib\jade.jar" part1.negotiation.AuctionLauncher unsold
java -cp "bin;lib\jade.jar" part2.Part2Launcher
```

Les executions JADE restent ouvertes volontairement pour montrer les agents dans la table JADE. Arreter avec `Ctrl+C` ou le bouton rouge `Terminate` dans Eclipse.

## Execution Python

```powershell
python "partie 3\run_planning_partial_order.py"
python "partie 4\part4_ap.py" --cli
```

Si necessaire :

```powershell
python -m pip install -r "partie 3\requirements.txt"
```

## Eclipse

Importer directement le dossier `projet tech-ag` avec `File > Open Projects from File System...`.

Les chemins `partie 1/src`, `partie 2/src` et `lib/jade.jar` sont deja configures dans `.classpath`.
