from PIL.Image import Image
import matplotlib.pyplot as plt
import numpy as np
import os
import pandas as pd
import config
from tensorflow.keras.models import load_model
from sklearn.preprocessing import LabelEncoder
from plot_predictions import *
import cv2
from sklearn.metrics import (balanced_accuracy_score, matthews_corrcoef, f1_score, 
ConfusionMatrixDisplay, confusion_matrix, roc_auc_score, RocCurveDisplay)

predict_archive = "/home/tasos/Downloads/archive_test/"
predict_resampled = "/home/tasos/Downloads/resampled_test/"

model = load_model("archive_fold_3")

filenames = os.listdir(predict_archive)
images = []
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)
    img_path = os.path.join(predict_archive, filename)
    img = cv2.imread(img_path)
    images.append(img)

images = np.array(images, dtype='float32')/255
images = images.reshape(images.shape[0], 168, 224, 3)
categories = np.array(categories)
le = LabelEncoder()

# for f1, MCC, Balanced_acc, CM
predictions = model.predict(images)

#for roc_auc_score
pred_scores = model.predict_proba(images)

y_pred = np.argmax(predictions, axis=-1)
y_true = le.fit_transform(categories)

print("f1_score: {}".format(f1_score(y_true, y_pred, average='weighted')))
print("MCC: {}".format(matthews_corrcoef(y_true, y_pred)))
print("Balanced_accuracy_score: {}".format(balanced_accuracy_score(y_true, y_pred)))

#Area Under Curve(AUC), gives the probability that a randomly chosen
#positive instance is ranked higher that a randomly chosen negative instance
#and acts as a useful metric in comparing classifiers and assessing their
#efectiveness for a given problem.
print("ROC_AUC_Score: {}".format(roc_auc_score(y_true, pred_scores, average='weighted', multi_class='ovr')))

#The ROC curve shows the trade-off between sensitivity (TPR)
#and specificity(1-FPR). Classifiers that give curves closer to the
#top-left corner indicate better performance. The closer to the 45-degree
#diagonal of the ROC space, the less accurate the classifier.
#ROC does not depend on class distribution, which is useful for our problem
disp = RocCurveDisplay.from_predictions(y_true, y_pred)
disp.plot()
plt.show()

# Display the confusion matrix for the model
cm = confusion_matrix(y_true, y_pred)
disp = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=config.CLASSES)
disp.plot()
plt.show()

"""# Plot the first X test images, their predicted labels, and the true labels.
# Color correct predictions in blue and incorrect predictions in red.
num_rows = 5
num_cols = 4
num_images = num_rows*num_cols
plt.figure(figsize=(2*2*num_cols, 2*num_rows))
for i in range(num_images):
    plt.subplot(num_rows, 2*num_cols, 2*i+1)
    plot_image(i, predictions[i], le.fit_transform(categories), images)
    plt.subplot(num_rows, 2*num_cols, 2*i+2)
    plot_value_array(i, predictions[i], le.fit_transform(categories))
plt.tight_layout()
plt.show()"""