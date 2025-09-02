Parfait 👍 Je vais te rédiger un **README clair et pédagogique** qui présente ton projet d’intégration d’une API externe (CurrencyAPI) avec **Spring WebFlux** et la programmation réactive.

---

# 📌 Intégration d’une API externe avec Spring WebFlux (Programmation Réactive)

## 🚀 Objectif

Ce projet montre comment intégrer une API tierce (ex. [CurrencyAPI](https://currencyapi.com)) dans une application **Spring Boot** en utilisant **Spring WebFlux**.

L’idée est de récupérer en continu les taux de change (ex. CAD → XOF) et de les exposer sous forme de flux réactifs.

---

## ⚙️ Technologies utilisées

* **Java 17+**
* **Spring Boot 3+**
* **Spring WebFlux** → programmation réactive non bloquante
* **Project Reactor (Flux / Mono)**
* **CurrencyAPI** (API externe de taux de change)
* **SSE (Server-Sent Events)** pour diffuser en continu les données
* **Docker / Postman** (pour les tests)

---

## 📂 Structure simplifiée du projet

```
apiintegration/
 ├── src/main/java/com/example/apiintegration
 │   ├── controller/        # Endpoints REST exposés
 │   ├── service/           # Logique métier (appel API externe)
 │   ├── model/             # DTO (ExchangeResponse)
 │   └── ApiIntegrationApp  # Classe principale Spring Boot
 ├── .env                   # Variables d’environnement (API Key)
 ├── pom.xml
 └── README.md
```

---

## 🔑 Variables d’environnement

On utilise un fichier `.env` (non versionné) pour stocker les secrets :

```
EXCHANGE_API_BASE_URL=https://api.currencyapi.com/v3/latest
EXCHANGE_API_KEY=VOTRE_CLE_API
```

⚠️ Ajoutez `.env` dans `.gitignore` pour éviter de versionner votre clé API.

---

## 🔄 Fonctionnement

### 1. Récupération automatique du taux de change

Le service `ReactiveCurrencyStreamService` interroge CurrencyAPI toutes les **30 secondes** pour mettre à jour le taux CAD → XOF.

```java
private void startRateStream() {
    Flux.interval(Duration.ofSeconds(30))
        .flatMap(tick -> fetchXafRate())
        .subscribe(rate -> latestRate.set(rate));
}
```

---

### 2. Exposition d’un **flux continu**

Un endpoint permet de diffuser le taux ou les conversions **en temps réel** via **SSE** :

```java
@GetMapping(value = "/convert-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<BigDecimal> convertStream(@RequestParam BigDecimal cadAmount) {
    return currencyStreamService.convertToXafStream(cadAmount);
}
```

---

### 3. Test avec Postman

1. Lancer l’application :

   ```
   mvn spring-boot:run
   ```
2. Faire un appel GET sur :

   ```
   http://localhost:8080/api/convert-stream?cadAmount=100
   ```
3. Dans Postman :

    * Onglet **Headers** → ajouter :

      ```
      Accept: text/event-stream
      ```
    * Après envoi, basculer sur l’onglet **Event Stream** pour voir les résultats toutes les **5 secondes**.

Exemple de sortie SSE :

```
data: 59874.32
data: 59902.11
data: 59890.77
```

---

## ✅ Avantages de WebFlux

* **Réactivité** : le serveur envoie les données dès qu’elles sont disponibles.
* **Non bloquant** : permet de gérer un grand nombre de connexions simultanément.
* **Flux continu** : idéal pour du temps réel (finance, IoT, monitoring…).

---

## 🔮 Améliorations possibles

* Stocker les taux dans une base réactive (MongoDB, R2DBC).
* Ajouter un cache pour limiter les appels à CurrencyAPI.
* Exposer plusieurs devises au lieu d’une seule (CAD → XOF, CAD → EUR, etc.).
* Interface web temps réel avec **React + EventSource**.

---

💡 **En résumé** : ce projet est un exemple concret d’**intégration d’une API externe en programmation réactive** avec **Spring WebFlux**.
Il montre comment transformer un service REST classique en un **flux continu de données** consommable par Postman ou un front web.

