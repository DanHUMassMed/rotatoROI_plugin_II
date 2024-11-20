# RotatoROI_II Plugin Setup Instructions

The **RotatoROI_IIPlugin** relies on a specific directory structure in order to find and process files.

### Step 1: Create a Root Directory
First, a root directory must be created. This is the location where the **RotatoROI_II** process will search for files and create the output.

This directory can be anywhere on the hard drive. It's best to use a naming convention that aligns with the overall experiment.
For example: `/Users/dan/MitoPaper_imageprocessing/2024-04-19/`

### Step 2: Create a "raw" Directory
Below the root directory, a directory named `raw` must exist. The content in this directory will contain the unprocessed (raw) images.

Within this `raw` directory, you must create additional directories that will contain the `.tif` images to be processed. The directories directly below the `raw` directory can have any name but should be related to the experimental conditions or subject of your images.

For example:
./raw/EV
./raw/sams-1

### Step 3: Place Images in the Proper Directories
Finally, the `.tif` images to be processed should be placed in these directories.

### Example Directory Structure
An image of an example directory structure is provided.

![root_directory_structure](root_directory_structure.png)

