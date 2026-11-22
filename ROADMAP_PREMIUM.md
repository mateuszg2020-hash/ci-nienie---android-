# ATMOS Premium — plan rozwoju

## Cel produktu
ATMOS ma być aplikacją pogodową klasy premium: prosty, czytelny ekran dla codziennego użytkownika oraz wiarygodne narzędzia dla osób, które chcą analizować pogodę. Priorytetem jest jakość danych, jasne oznaczanie prognoz modelowych i brak atrap funkcjonalności.

## Zasady produktu
- Każda liczba ma pochodzić z rzeczywistego źródła danych; brak danych pokazujemy jako brak danych, nie jako zero.
- Rozdzielamy obserwację (radar/stacje) od prognozy modelowej.
- Pokazujemy czas aktualizacji, źródło i ograniczenia prognozy.
- Zmiana lokalizacji odświeża wszystkie moduły, mapy i ostrzeżenia.
- Funkcje krytyczne mają obsługę błędów, timeout i stan ładowania.
- Nie publikujemy APK jako „gotowego”, dopóki workflow nie przejdzie i artefakt nie zostanie sprawdzony.

## Etap 0 — fundament i jakość (w toku)
- [x] Uporządkowanie HTML/CSS/JS w osobnych assets.
- [x] Premium visual system i przebudowa ekranu głównego.
- [x] Kontrola składni JS w CI i kompilacja APK.
- [x] Karta decyzji „Czy brać parasol?” z godzinowym rozkładem opadów.
- [x] Odświeżanie modułów powietrza/ostrzeżeń/słońca po zmianie lokalizacji.
- [ ] Ujednolicenie cache i odświeżania danych; widoczny stan offline.
- [ ] Testy na urządzeniu: obrót, powrót z tła, utrata sieci, zmiana miasta, mały ekran.

## Etap 1 — wiarygodna pogoda i alerty
- [ ] Radar obserwacyjny RainViewer: aktualne klatki radarowe, legenda, oś czasu; wyraźnie oddzielony od prognozy.
- [ ] Mapa prognozy opadów na przyszłość jako osobna warstwa/model, bez nazywania jej radarem.
- [ ] Nowcast opadów: interwał możliwie krótki, prawdopodobieństwo, intensywność i czas rozpoczęcia/ustania.
- [ ] Ostrzeżenia IMGW dopasowane po kodzie TERYT dla każdej wybranej lokalizacji, nie po ręcznej liście miast.
- [ ] Centrum burzowe: wyładowania/ostrzeżenia tylko z wiarygodnego źródła, z czasem aktualizacji.
- [ ] Powiadomienia działające także poza otwartą aplikacją (Android WorkManager/FCM lub lokalny harmonogram); uprawnienia i ograniczenia jasno opisane.

## Etap 2 — prognoza premium
- [ ] Porównanie modeli ECMWF, GFS i ICON dla temperatury, opadów i wiatru.
- [ ] Rozbieżność modeli i zakres niepewności zamiast jednej pozornie precyzyjnej wartości.
- [ ] Prognoza godzinowa 48–72 h z przełączanymi parametrami.
- [ ] Dni 1–16 jako prognoza dzienna; 17–45 wyłącznie trend sezonowy, wyraźnie oznaczony jako orientacyjny.
- [ ] Poprawne ikony i krótkie opisy także w trendzie tygodniowym.

## Etap 3 — mapy i meteorologia
- [ ] Mapa wiatru z rzeczywistymi wektorami siatki modelu, a nie jedną lokalną wartością rozciągniętą na mapę.
- [ ] Warstwy: temperatura, zachmurzenie, opady, ciśnienie i porywy wiatru.
- [ ] Oś czasu, legenda jednostek, źródło i czas modelu na każdej mapie.
- [ ] Historia ciśnienia 24 h / 7 dni / 30 dni, z rozróżnieniem pomiaru i modelu.

## Etap 4 — codzienna użyteczność
- [ ] Widżety Androida: kompaktowy, godzinowy i ciśnieniowy.
- [ ] Zapisane lokalizacje i szybkie porównanie kilku miast.
- [ ] Jakość powietrza: PM2.5, PM10, AQI UE/USA i pyłki; kategorie opisane zgodnie ze skalą źródła.
- [ ] UV, wschód/zachód słońca, długość dnia i faza Księżyca.
- [ ] Pogoda dla kierowcy: mgła, oblodzenie, śnieg, porywy i ryzyko trudnych warunków.
- [ ] Warunki na wodzie jako moduł orientacyjny, bez udawania prognozy fal tam, gdzie źródło ich nie dostarcza.

## Etap 5 — warstwa premium i dopracowanie
- [ ] ATMOS Score z jawną metodologią i osobnymi ocenami: ogólna, outdoor, drive, boat.
- [ ] Inteligentne podsumowanie dnia: co się zmieni i kiedy.
- [ ] Personalizowane progi alertów.
- [ ] Widżety, dostępność, lokalizacja offline ostatnich danych i oszczędzanie baterii.
- [ ] Testy regresji i finalna kontrola UX na kilku rozmiarach ekranów.

## Kryteria wydania
1. Build CI przechodzi.
2. JS bez błędów składni, brak brakujących elementów DOM.
3. Wszystkie moduły aktualizują się po zmianie lokalizacji.
4. Brak mylenia prognozy z obserwacją.
5. Test urządzeniowy: uruchomienie, minimalizacja, powrót, brak internetu, ponowne połączenie.
6. APK podpisany kluczem wydania przed publiczną dystrybucją.
