"""
Partie H - Detection de fraude bancaire (classification binaire)

Etapes :
 1. Chargement des donnees
 2. Proportion de fraudes et valeurs manquantes
 3. Separation entrainement / test STRATIFIEE
 4. Exclusion des identifiants (transaction_id, account_id)
 5. Pipeline : imputation + encodage one-hot + normalisation
 6. Modeles simples : regression logistique et random forest
 7. Evaluation : precision et rappel de la classe fraude, F1, matrice de confusion, average precision

Regles respectees :
 - le pretraitement est ajuste UNIQUEMENT sur les donnees d'entrainement (il est dans le Pipeline) ;
 - l'ensemble de test ne sert ni a entrainer, ni a choisir le modele, ni a choisir le seuil :
   le seuil est choisi sur un jeu de VALIDATION extrait de l'entrainement.

Execution :  python fraud_detection.py
"""
import json
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (ConfusionMatrixDisplay, PrecisionRecallDisplay, average_precision_score,
                             classification_report, confusion_matrix, f1_score, precision_recall_curve,
                             precision_score, recall_score)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

BASE = Path(__file__).resolve().parent
DATA = BASE / "data" / "bank_transactions.csv"
OUT = BASE / "outputs"
OUT.mkdir(exist_ok=True)
RANDOM_STATE = 42
ID_COLS = ["transaction_id", "account_id"]
TARGET = "is_fraud"


def title(text):
    print("\n" + "=" * 70 + f"\n{text}\n" + "=" * 70)


# ------------------------------------------------------------------ 1. Chargement
title("1. CHARGEMENT DES DONNEES")
df = pd.read_csv(DATA)
print(f"Dimensions : {df.shape[0]} lignes x {df.shape[1]} colonnes")
print(df.head())
print("\nTypes :")
print(df.dtypes)

# ------------------------------------------------------------------ 2. Fraudes + valeurs manquantes
title("2. PROPORTION DE FRAUDES ET VALEURS MANQUANTES")
counts = df[TARGET].value_counts().sort_index()
props = df[TARGET].value_counts(normalize=True).sort_index()
print(pd.DataFrame({"effectif": counts, "proportion (%)": (props * 100).round(2)}))
print(f"\n=> Taux de fraude : {props.get(1, 0) * 100:.2f} % (classes desequilibrees)")

missing = pd.DataFrame({"manquants": df.isna().sum(), "pourcentage (%)": (df.isna().mean() * 100).round(2)})
print("\nValeurs manquantes par colonne :")
print(missing)

# ------------------------------------------------------------------ 3-4. Split stratifie + exclusion des IDs
title("3-4. SEPARATION STRATIFIEE ET EXCLUSION DES IDENTIFIANTS")
X = df.drop(columns=ID_COLS + [TARGET])
y = df[TARGET].astype(int)
num_cols = X.select_dtypes(include="number").columns.tolist()
cat_cols = X.select_dtypes(exclude="number").columns.tolist()
print(f"Identifiants exclus      : {ID_COLS}")
print(f"Variables numeriques     : {num_cols}")
print(f"Variables categorielles  : {cat_cols}")

# Test = 20 % (mis de cote, jamais utilise avant l'evaluation finale)
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.20, stratify=y, random_state=RANDOM_STATE)
# Validation = 25 % de l'entrainement (choix du modele et du seuil)
X_tr, X_val, y_tr, y_val = train_test_split(
    X_train, y_train, test_size=0.25, stratify=y_train, random_state=RANDOM_STATE)

for name, part in [("train", y_tr), ("validation", y_val), ("test", y_test)]:
    print(f"{name:<11}: {len(part):>6} lignes | taux de fraude = {part.mean() * 100:.2f} %")


# ------------------------------------------------------------------ 5. Pipeline
def build_preprocessor():
    numeric = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler()),
    ])
    categorical = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("onehot", OneHotEncoder(handle_unknown="ignore")),
    ])
    return ColumnTransformer([("num", numeric, num_cols), ("cat", categorical, cat_cols)])


models = {
    "LogisticRegression": LogisticRegression(max_iter=2000, class_weight="balanced"),
    "RandomForest": RandomForestClassifier(n_estimators=300, min_samples_leaf=2,
                                           class_weight="balanced_subsample",
                                           random_state=RANDOM_STATE, n_jobs=-1),
}


def best_f1_threshold(y_true, proba):
    precision, recall, thresholds = precision_recall_curve(y_true, proba)
    f1 = 2 * precision * recall / (precision + recall + 1e-12)
    idx = int(np.argmax(f1[:-1]))
    return float(thresholds[idx]), float(f1[idx])


# ------------------------------------------------------------------ 6. Entrainement + selection sur validation
title("5-6. PIPELINE + ENTRAINEMENT (selection sur VALIDATION)")
val_results = {}
for name, model in models.items():
    pipe = Pipeline([("preprocessing", build_preprocessor()), ("model", model)])
    pipe.fit(X_tr, y_tr)
    proba_val = pipe.predict_proba(X_val)[:, 1]
    ap = average_precision_score(y_val, proba_val)
    thr, f1 = best_f1_threshold(y_val, proba_val)
    val_results[name] = {"average_precision": ap, "threshold": thr, "f1_at_threshold": f1}
    print(f"{name:<20} AP(val) = {ap:.4f} | seuil optimal F1 (val) = {thr:.3f} | F1(val) = {f1:.4f}")

best_name = max(val_results, key=lambda n: val_results[n]["average_precision"])
threshold = val_results[best_name]["threshold"]
print(f"\n=> Modele retenu (meilleure AP en validation) : {best_name}")
print(f"=> Seuil de decision retenu (choisi sur la validation) : {threshold:.3f}")

# Re-entrainement du modele retenu sur tout l'entrainement (train + validation), le test reste intact
final_model = Pipeline([("preprocessing", build_preprocessor()), ("model", models[best_name])])
final_model.fit(X_train, y_train)

# ------------------------------------------------------------------ 7. Evaluation finale sur le TEST
title(f"7. EVALUATION FINALE SUR LE TEST - {best_name}")
proba_test = final_model.predict_proba(X_test)[:, 1]
ap_test = average_precision_score(y_test, proba_test)
summary = {"model": best_name, "average_precision_test": round(ap_test, 4),
           "fraud_rate_dataset": round(float(props.get(1, 0)), 4), "validation": val_results}

for label, thr in [("seuil par defaut 0.5", 0.5), (f"seuil choisi en validation {threshold:.3f}", threshold)]:
    y_pred = (proba_test >= thr).astype(int)
    p = precision_score(y_test, y_pred, zero_division=0)
    r = recall_score(y_test, y_pred, zero_division=0)
    f = f1_score(y_test, y_pred, zero_division=0)
    cm = confusion_matrix(y_test, y_pred)
    tn, fp, fn, tp = cm.ravel()
    print(f"\n--- {label} ---")
    print(f"Precision (fraude) : {p:.4f}")
    print(f"Rappel    (fraude) : {r:.4f}")
    print(f"F1-score  (fraude) : {f:.4f}")
    print(f"Average precision  : {ap_test:.4f}")
    print(f"Matrice de confusion :\n{cm}")
    print(f"  TN={tn}  FP={fp} (fausses alertes)  FN={fn} (fraudes non detectees)  TP={tp}")
    print(classification_report(y_test, y_pred, target_names=["legitime", "fraude"], zero_division=0))
    summary[label] = {"threshold": round(thr, 4), "precision": round(p, 4), "recall": round(r, 4),
                      "f1": round(f, 4), "TN": int(tn), "FP": int(fp), "FN": int(fn), "TP": int(tp)}

# ------------------------------------------------------------------ Graphiques
fig, axes = plt.subplots(1, 2, figsize=(11, 4.5))
for ax, (label, thr) in zip(axes, [("Seuil 0.5", 0.5), (f"Seuil {threshold:.2f} (validation)", threshold)]):
    ConfusionMatrixDisplay(confusion_matrix(y_test, (proba_test >= thr).astype(int)),
                           display_labels=["legitime", "fraude"]).plot(ax=ax, cmap="Blues", colorbar=False)
    ax.set_title(f"{best_name} - {label}")
plt.tight_layout()
plt.savefig(OUT / "confusion_matrix.png", dpi=130)

fig, ax = plt.subplots(figsize=(6, 4.5))
PrecisionRecallDisplay.from_predictions(y_test, proba_test, ax=ax, name=best_name)
ax.axhline(y_test.mean(), linestyle="--", color="grey", label=f"Hasard (taux de fraude = {y_test.mean():.3f})")
ax.set_title(f"Courbe precision-rappel (test) - AP = {ap_test:.3f}")
ax.legend()
plt.tight_layout()
plt.savefig(OUT / "precision_recall_curve.png", dpi=130)

if best_name == "RandomForest":
    names = final_model.named_steps["preprocessing"].get_feature_names_out()
    imp = pd.Series(final_model.named_steps["model"].feature_importances_, index=names).sort_values(ascending=False)
    print("\nTop 10 variables les plus importantes :")
    print(imp.head(10).round(4))
    imp.head(10).sort_values().plot.barh(figsize=(7, 4.5), title="Importance des variables (Random Forest)")
    plt.tight_layout()
    plt.savefig(OUT / "feature_importance.png", dpi=130)

(OUT / "metrics.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
print(f"\nResultats sauvegardes dans : {OUT}")