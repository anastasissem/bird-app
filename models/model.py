import os
import time
import sys

spec_path = "/home/tasos/iot-analytics/archive/train_2D/"

sys.path.append("/home/tasos/iot-analytics/preprocess_images/")

import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras.optimizers import Adam
from sklearn.model_selection import train_test_split
from tensorflow.keras.callbacks import (EarlyStopping, ReduceLROnPlateau,
                                        TensorBoard)
from tensorflow.keras.layers import (Activation, BatchNormalization, Conv2D,
                                     Dense, Dropout, Flatten, MaxPooling2D)
from tensorflow.keras.models import Sequential

from load_specs_2D import get_specs
from png2jpg import compress


# Check if all images have been compressed and shrinked
for file in os.listdir(spec_path):
    if not file.endswith(".jpg"):
        compress(spec_path)
    break

np_images, np_labels = get_specs(spec_path)
x_train, x_test, y_train, y_test = train_test_split(np_images, np_labels, test_size=0.2)


# Build Model
model = Sequential()

#model.add(Conv2D(32, (3,3), input_shape=(168, 224, 3)))
model.add(Conv2D(32, (3,3), input_shape=(168, 224, 1)))

model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Conv2D(64, (3,3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Conv2D(128, (3,3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Flatten())
model.add(Dense(50, activation='softmax'))

model.summary()

#Compile Model
opt = Adam(lr=0.001)
model.compile(optimizer=opt,
loss='categorical_crossentropy', metrics=['accuracy'])

#Callbacks
early_stop = EarlyStopping(monitor='val_loss', patience=5)
lr_redux = ReduceLROnPlateau(monitor='val_loss', patience=3,
                            verbose=1, 
                            factor=0.1, 
                            min_lr=0.00001)

log_dir = f"/home/tasos/iot-analytics/logs/{int(time.time())}"                            
tensorboard = TensorBoard(log_dir = log_dir)
callbacks = [early_stop, lr_redux, tensorboard]


#Fit Model
history = model.fit(x_train,
                    y_train,
                    epochs=10,
                    validation_split=0.2)

# Evaluate model
test_loss, test_acc = model.evaluate(x_test, y_test)

print("Test accuracy: {}" .format(test_acc))
print("Test loss: {}" .format(test_loss))