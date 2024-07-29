## Third-Party Libraries

This project uses the following third-party libraries:

- [json-simple](https://code.google.com/archive/p/json-simple/) - Licensed under the Apache License 2.0

# kmkss_img_tool
 An extraction tool for a certain ps2 visualnovel
 
 //note: havent fully tested this ingame tho it boots the game


This project is based on amg_codec_tool's documentation (rev 0.5)
https://code.google.com/archive/p/amg-codec-tool/
and applied to java to make a custom extraction program for
amg's sister game kmkss


//refer in amg_codec_tool's doc for indepth information about the structure of ps2 iso and files associated with it.


## How to use:

After opening the program:

Parameters

1 : ARC file to JSON

2 : JSON to SCFs

3 : SCFs to ARC:

4 //debug dont use:

5 : IMG|PAC to ARC

6 : ARC to IMG

## Parameters

### Parameter 1:

This will ask for an arc file to be extracted to JSON and scfTemp files in JSONextract and SCFextract folders.
This will also make tmp.list files which will list all scffile names based on the arc file. 


### Parameter 2:

This will ask for a folder cotaining JSON files and convert it to scf Files and be saved on SCFconvert


### Parameter 3:

Will ask for folder containing SCF files to be converted to ARC file
Filename: SCF_to_ARC.ARC


### Parameter 5:

Will ask for IMG file to be conveted to arc File
Filename: IMG_to_ARC.ARC


### Parameter 6:

Will ask for ARC file to be converte to IMG file
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
toTranslate is a boolean 0|1 that indicate for the program to use tdata instean xdata when using parameter 2






## SPECIAL THANKS (CHECK THEM OUT!)

nishishitranslations - https://nishishitranslations.wordpress.com/

freesmiler - https://freesmiler.livejournal.com/

comet_27 - https://sites.google.com/view/amagamiebkoretranslations/

and others contributed for amagami translations



## to do

//will fix this readme in december sorry for bed england

