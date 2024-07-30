## Third-Party Libraries

This project uses the following third-party libraries:

- [json-simple](https://code.google.com/archive/p/json-simple/) - Licensed under the Apache License 2.0

# kmkss_img_tool
 An extraction tool for a certain PS2 visual novel
 
 **CAN'T GUARANTEE TO WORK HAVENT FULLY TESTED A FULL GAME**

**USE AT YOUR OWN RISK**


This project is based on amg_codec_tool's documentation (rev 0.5)
https://code.google.com/archive/p/amg-codec-tool/
and applied to Java to make a custom extraction program for
amg's sister game kmkss


//refer to amg_codec_tool's doc for in-depth information about the structure of ps2 iso and files associated with it.


## How to use:

Use vscode or any ide and run the main file

Parameters

1: ARC file to JSON

2: JSON to SCFs

3: SCFs to ARC:

4 //debug don't use:

5: IMG|PAC to ARC

6: ARC to IMG

## Parameters

### Parameter 1:

Takes an arc file to be extracted to JSON and scfTemp files in JSONextract and SCFextract folders.
This also make tmp.list files which will list all scf file names based on the arc file. 


### Parameter 2:

Takes folder directory cotaining JSON files and convert it to scf Files and be saved on SCFconvert


### Parameter 3:

Takes folder directory containing SCF files to be converted to ARC file
Filename: SCF_to_ARC.ARC


### Parameter 5:

Takes an IMG file to be conveted to arc File
Filename: IMG_to_ARC.ARC


### Parameter 6:

Takes an ARC file to be converte to IMG file
Filename: ARC_to_IMG.IMG



## JSON FILES 

Just like in amg_codec_tool the project have a human readable file format for editing scripts for translation in json format
and also has similar format as the xml in amgtool


Format:

Variables[ ]

Block[ ]

Unknown[ ]

Labels[ ]

Entries[ ]

## Type 5 entries
Each type 5 entries (amgdoc page:8) will look like this:


{

      "index": 1,
      
      "xdata": "日本語上手",
      
      "type": 5,
      
      "tdata": "ＮｏＤａｔａ",
      
      "toTranslate": 0
      
}

the xdata is the original text, tdata is the text that will replace xdata
toTranslate is a boolean 0|1 that indicate for the program to use tdata instead of xdata when using parameter 2

Recommend Full Width for alphanumeric 

There are special expressions in xdata that could be used in tdata
V00000 - voices

Ts1 Ts2 .. etc for text size




## SPECIAL THANKS (CHECK THEM OUT!)

nishishitranslations - https://nishishitranslations.wordpress.com/

freesmiler - https://freesmiler.livejournal.com/

comet_27 - https://sites.google.com/view/amagamiebkoretranslations/

and others contributed for amagami translations



## to do

//will fix this readme in december sorry for bed england

