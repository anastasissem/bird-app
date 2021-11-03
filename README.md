This repository contains code for an Android Application for Birdsong Recognition

The folder 'download xeno-canto' contains files that are used to extract recordings from the xeno-canto platform. This is necessary
since there are not commercial birdsong dataset that fit my needs. Bulk data is downloaded from the site and then a specific number of 
the desired classes is extracted from the bulk .csv data. Finally, the names of the recordings are fixed to support the format used in 
the scripts.

Preprocess_recordings contains three files. Load.py is used to load each recording and downsample it to reduce space on disk with no effect
on recording quality. Split_wav.py splits each recording to 5 second chunks and the ones deemed to contain noise instead of actual birdsong 
are discarded by comparing chunk power/energy to the song's total. Then, Preprocessing.py sets the parameters for STFT and creates spectrograms
for each chunk and saves them on disk.

Preprocess_images contains the script png2jpg.py, which converts the png matplotlib spectrograms to jpg images, while also resizing them
to achieve smaller files and better speed. The remaining scripts are previous attempts that were droped out.

'models' folder contains slightly different methods in training the CNN. model.py was abandoned because of large memory consumption. All training
attempts where conducted on model_kfold.py which employs stratified cross validation to combat class imbalance. Predictions were made
on each saved fold in terms of accuracy, f1_score, balanced_accuracy, confusion matrix, MCC and roc_auc_score.
