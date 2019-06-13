# Simpler Webserver | curl-http-tester

Einfache HTTP/1.0-Tests für das Webserver-Praktikum der Veranstaltung [Rechnernetze](https://www.tsn.hhu.de/teaching/lectures/2018ws/rechnernetze.html) im WS2018/19.
Wir verwenden zum Testen Java 1.8.

## Voraussetzungen:

Auf dem System müssen folgende Programme installiert sein:
- `curl`
- `java`
- `javac`

## Verwendung

Kopieren Sie ihre `WebServer.java`-Datei in den selben Ordner, in welchem sich die Dateien dieses Projekts befinden.
Dann können Sie die Tests mit `./curl_tests.sh` ausführen lassen.

**Wichtig:** Die HTTP-Statuscodes in der Ausgabe ( *should deliver* ) beziehen sich auf die zu erwartende Response, falls der Request-Typ implementiert wurde.
Die geforderten Request-Typen sind der Aufgabenstellung zu entnehmen.

## Beispiel

**Hinweis:** Die Ausgaben des Webservers wurden hier entfernt, lediglich die Header werden dargestellt.

```
curl-http-tester$ cp /path/to/WebServer.java .

curl-http-tester$ ls
curl_tests.sh  index.htm  mime.types  WebServer.java

curl-http-tester$ ./curl_tests.sh
[+] Compiling source
[+] Running program
[+] Wait 3s for program to start...
[+] Testing HTTP/1.0 GET
  [+] Nonexisting file (Should deliver 404)
HTTP/1.0 404 Not Found
Content-type: text/html

  [+] Existing mime.types-file (Should deliver 200)
HTTP/1.0 200 OK
Content-type: application/octet-stream

  [+] Java source (Should deliver 200 and correct MIME type)
HTTP/1.0 200 OK
Content-type: text/x-java


[+] Testing HTTP/1.0 HEAD
  [+] Nonexisting file (Should deliver 404)
HTTP/1.0 404 Not Found
Content-type: text/html

  [+] Existing file (Should deliver 200 without content)
HTTP/1.0 200 OK
Content-type: text/html


[+] Testing HTTP/1.0 POST
  [+] Nonexisting file (Should deliver 404)
HTTP/1.0 404 Not Found
Content-type: text/html

  [+] Existing file (Should deliver 200)
HTTP/1.0 200 OK
Content-type: text/html

  [+] Nonexisting file and invalid request (Should deliver 400)
HTTP/1.0 400 Bad Request
Content-type: text/html

  [+] Existing file and invalid request (Should deliver 400)
HTTP/1.0 400 Bad Request
Content-type: text/html

  [+] Nonexisting file and valid request (Should deliver 404)
HTTP/1.0 404 Not Found
Content-type: text/html

  [+] Existing file and valid request (Should deliver 200 with content OR 204 without content)
HTTP/1.0 204 No Content
Content-type: text/html

[+] Stopping program
```
