# 2fa2Notification

Application Android qui détecte les codes 2FA reçus **par SMS** ou **par mail Gmail** et les affiche dans une notification permanente, avec un bouton **Copier**.

## Fonctionnalités

- **Codes SMS** : détection en temps réel à la réception, et analyse de la boîte de réception des 10 dernières minutes.
- **Codes Gmail** : lecture seule via l'API Gmail des mails reçus dans les 10 dernières minutes. La corbeille et les spams sont exclus.
- **Notification permanente** (service de premier plan) :
  - le dernier code trouvé, avec un bouton **Copier** ;
  - l'expéditeur et la date de réception du message ;
  - un bouton **Vérifier**.
- **Vérification à la demande** : au clic sur **Vérifier**, jusqu'à 5 tentatives espacées de 5 secondes (SMS puis mails). La vérification s'arrête dès qu'un nouveau code est trouvé. Elle tourne en arrière-plan et ne bloque pas l'interface.
- **Aucune interrogation automatique** : l'app ne lit rien entre deux demandes. Seuls les SMS entrants sont reçus en direct.
- **Redémarrage** : la notification est relancée automatiquement après un redémarrage du téléphone.
- **Écran principal** :
  - état de l'autorisation Gmail ;
  - état des autorisations SMS et notifications ;
  - état de l'optimisation batterie, avec un bouton pour la désactiver ;
  - bouton pour analyser les SMS ;
  - journal de débogage.

## Prérequis

- Android 8.0 (API 26) ou plus.
- Un projet Google Cloud avec la **Gmail API** activée, pour la lecture des mails.

## Configuration Gmail (une seule fois)

1. Dans [Google Cloud Console](https://console.cloud.google.com), crée un projet et active la **Gmail API**.
2. Configure l'**écran de consentement OAuth** :
   - ajoute le scope `https://www.googleapis.com/auth/gmail.readonly` ;
   - ajoute ton adresse Gmail dans les **utilisateurs de test**.
3. Crée un **ID client OAuth de type Android** dans le même projet :
   - nom du package : `com.neissa.twofa2notification` ;
   - empreinte SHA-1 : celle de ta clé de debug (`gradlew signingReport`).

> [!NOTE]
> En mode test, l'autorisation Google expire au bout de 7 jours : il faudra alors la refaire depuis l'app.

## Installation

```bash
gradle :app:installDebug
```

## Utilisation

1. Ouvre l'app et accorde les autorisations **SMS** et **notifications**.
2. Appuie sur **Se connecter à Gmail (API)** et accepte la lecture seule.
3. Désactive l'**optimisation batterie** pour l'app, avec le bouton prévu à cet effet.
4. Quand un code 2FA est attendu, appuie sur **Vérifier** dans la notification, ou reçois-le directement par SMS.
5. Appuie sur **Copier** pour mettre le code dans le presse-papiers.

## Confidentialité

- Les SMS et mails ne sont lus que pour y chercher un code 2FA récent. Rien n'est envoyé à un serveur.
- L'accès Gmail est en lecture seule.
