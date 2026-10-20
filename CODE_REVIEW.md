# Code Review Report: RotatoROI_II Plugin

**Repository**: `rotatoROI_plugin_II`  
**Review Target**: Full codebase (`app/src/main`, `app/src/test`, build configuration)  
**Standard**: Multi-Axis Evaluation (Correctness, Readability, Architecture/SOLID, Security, Performance, Test & Build Health)

---

## Executive Summary

[RotatoROI_II](file:///Users/dan/Code/groovy/rotatoROI_plugin_II) is a functional Fiji/ImageJ plugin implementing a 3-step semi-automated image processing pipeline (Bio-Formats `.nd2` channel splitting, synchronized multi-channel stack rotation, and standardized ROI cropping).

While the core image processing workflow accomplishes its scientific goals, the codebase exhibits critical compilation failures in the test suite, platform portability defects, an unthrottled CPU busy loop, class naming discrepancies, and substantial duplication across directory traversal classes.

| Evaluation Axis | Rating | Key Finding |
| :--- | :---: | :--- |
| **Correctness & Reliability** | **Needs Improvement** | Broken test suite compilation, platform-dependent path separators (`/`), unsafe string slicing for file extensions. |
| **Readability & Simplicity** | **Fair** | Clear intent, but contains informal developer artifacts (`WTF` comments), mismatched class/file names, and unused orphaned classes (`GLDataframe`). |
| **Architecture (SOLID)** | **Fair** | Duplication between `ProcessDirectory2` and `ProcessChannelSplitDirectory`; hardcoded channel constants violate OCP. |
| **Security & Data Safety** | **Moderate** | Unanchored file deletion in `deleteUnknownFiles`; missing validation for path traversal on user directory input. |
| **Performance & Concurrency** | **Needs Improvement** | Unthrottled thread loop (`while(dialog.isVisible())`) burns 100% CPU during progress intervals; blocking wait loops. |
| **Build & Toolchain** | **Good** | Modern Gradle 8.10.1 setup with Foojay toolchain resolver, but requires explicit JDK 17+ host for the build daemon. |

---

## 1. Correctness & Bug Risks

### 1.1 [Critical] Broken Test Suite Prevents Build & CI (`./gradlew test`)
- **Files**:
  - [CalculatorSpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/CalculatorSpec.groovy#L9)
  - [ProcessDirectorySpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/process/ProcessDirectorySpec.groovy#L8)
- **Problem**: Running `./gradlew test` fails immediately:
  1. `CalculatorSpec.groovy` attempts to instantiate a non-existent `Calculator` class (leftover from default `gradle init`).
  2. `ProcessDirectorySpec.groovy` imports `com.amywalkerlab.rotato_roi_ii.process.RotateImages` instead of [RotateImages2](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/RotateImages2.groovy#L11).
  3. `ProcessDirectorySpec.groovy` references hardcoded local paths (`/Users/dan/Code/Image-J/MitoExperiments/2024-04-19`) and uses no-op assertions (`true == true`).
- **Recommendation**:
  - Remove [CalculatorSpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/CalculatorSpec.groovy).
  - Refactor [ProcessDirectorySpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/process/ProcessDirectorySpec.groovy) to use Spock's `@TempDir` to test path manipulation and directory structure validation methods without depending on machine-local directories.

### 1.2 [High] Platform Portability Defect in Path Replacement (Windows Incompatibility)
- **Files**:
  - [RotateImages2.groovy:L26-L29](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/RotateImages2.groovy#L26-L29)
  - [CropImages.groovy:L32-L35](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/CropImages.groovy#L32-L35)
- **Problem**:
  ```groovy
  filePath = filePath.replace("/" + from + "/", "/" + to + "/");
  ```
  On Windows, `filePath` uses `\` (`File.separator`). Consequently, replacing `"/" + from + "/"` silently fails to change the directory path, leading to `FileNotFoundException` or writing files to incorrect directories.
- **Recommendation**:
  Use `File.separator` or regex with `java.nio.file.Path`:
  ```groovy
  filePath = filePath.replace(File.separator + from + File.separator, File.separator + to + File.separator)
  ```

### 1.3 [High] Extension Slicing Hazard in ND2 Processing
- **File**: [ND2Images.groovy:L60, L68, L75](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ND2Images.groovy#L60)
- **Problem**:
  ```groovy
  fileName = file.getName()[0..-5] + "_DIC.tif"
  ```
  String indexing `[0..-5]` assumes the file extension is exactly 4 characters (`.nd2`). If a file has uppercase `.ND2`, or an irregular suffix, or an unexpected name, this will truncate file characters or crash with index exceptions.
- **Recommendation**:
  ```groovy
  def baseName = file.name.replaceFirst(/\.[^.]+$/, '')
  fileName = "${baseName}_DIC.tif"
  ```

### 1.4 [Medium] Missing Return Value on Error Path
- **File**: [ND2Images.groovy:L43-L46](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ND2Images.groovy#L43-L46)
- **Problem**:
  ```groovy
  if (imagePluses == null) {
      IJ.error("Error: Expected an nd2 file")
      return
  }
  ```
  The method contract requires returning `boolean` (whether to terminate). Bare `return` yields Groovy default `null`, which evaluates to falsy when checked, masking termination behavior.
- **Recommendation**: Return `true` to signal termination or throw [RotatoROI2Exception](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/RotatoROI2Exception.groovy#L3).

### 1.5 [High] Hazardous Unanchored File Deletion Logic
- **File**: [ProcessDirectory2.groovy:L98-L108](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ProcessDirectory2.groovy#L98-L108)
- **Problem**:
  ```groovy
  private void deleteUnknownFiles(List<String> toBeRemoved) {
      for (String fileName : toBeRemoved) {
          Path filePath = Paths.get(fileName); // fileName is only a bare name like "specimen1.tif"!
          Files.deleteIfExists(filePath);
      }
  }
  ```
  `toBeRemoved` contains bare file names returned by `file.getFileName().toString()`. Calling `Paths.get(fileName)` resolves against the **current working directory** of Fiji/Java, NOT the secondary directory! If a file with the same name exists in the CWD, it could be deleted.
- **Recommendation**:
  Pass the parent directory path: `Paths.get(secondaryDir, fileName)`.

---

## 2. Readability & Simplicity

### 2.1 [Important] Class Name and Application Entry Point Mismatch
- **Files**:
  - [RotatoROI_II.groovy:L27](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/RotatoROI_II.groovy#L27)
  - [app/build.gradle:L71](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/build.gradle#L71)
- **Problem**:
  - The source file is named `RotatoROI_II.groovy`.
  - The class declared inside is `class Rotato_ROI_II implements PlugIn` (extra underscore).
  - `build.gradle` defines `mainClass = 'com.amywalkerlab.rotato_roi_ii.RotatoROI_II'` (no underscore).
  - `plugins.config` uses `com.amywalkerlab.rotato_roi_ii.Rotato_ROI_II`.
- **Recommendation**:
  Align class name and file name to `RotatoROI_II` across [RotatoROI_II.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/RotatoROI_II.groovy), [plugins.config](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/resources/plugins.config#L8), and [build.gradle](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/build.gradle#L71).

### 2.2 [Nit] Informal Comments and Misleading Log Tags
- **Files**:
  - [RotateImages2.groovy:L36](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/RotateImages2.groovy#L36): `// Get the number of slices WTF`
  - [CropImages.groovy:L54](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/CropImages.groovy#L54): `// Make the image that was passed in the active image WTF`
  - [RotatoROI_II.groovy:L180](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/RotatoROI_II.groovy#L180): `debugLogger.debug("RotateImages terminateProcess="+terminateProcess)` under `CropImages` execution block.
- **Recommendation**: Remove expletives/debug notes and correct log message labels.

### 2.3 [Optional] Orphaned Classes (`GLDataframe`)
- **Package**: [io.github.dphiggs01.gldataframe](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/io/github/dphiggs01/gldataframe)
- **Problem**: [GLDataframe.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/io/github/dphiggs01/gldataframe/GLDataframe.groovy) (600+ lines) is completely unreferenced by the plugin pipeline. Only [GLLogger.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/io/github/dphiggs01/gldataframe/utils/GLLogger.groovy) is utilized.
- **Recommendation**: If `GLDataframe` is an independent utility library, publish or extract it as an external dependency rather than keeping unused tabular processing code in the plugin repository.

---

## 3. Architecture & SOLID Principles

### 3.1 Single Responsibility Principle (SRP)
- In [RotateImages2.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/RotateImages2.groovy) and [CropImages.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/CropImages.groovy), the `processFile` method mixes:
  1. Opening file resources and displaying ImagePlus instances
  2. Dialog event polling and user interaction logic
  3. Image pixel transformation and math
  4. Disk serialization and path construction
- **Recommendation**: Decouple the UI interaction (`WaitForUserDialog`) from the transformation service (`ImageTransformService.rotateStack`, `ImageTransformService.cropStack`).

### 3.2 Open / Closed Principle (OCP) - Hardcoded Channels
- Channels are hardcoded across multiple classes as `"DIC"`, `"488"`, and `"561"`.
- If an experiment has DAPI (405 nm) or Cy5 (647 nm), the entire plugin fails validation in [ProcessChannelSplitDirectory.groovy:L63](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ProcessChannelSplitDirectory.groovy#L63).
- **Recommendation**: Introduce a `ChannelConfig` domain model or preference option allowing users to define active channels (e.g. `["DIC", "488", "561"]`).

### 3.3 Code Duplication: Inheritance Architecture
- [ProcessDirectory2.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ProcessDirectory2.groovy) and [ProcessChannelSplitDirectory.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ProcessChannelSplitDirectory.groovy) duplicate almost identical directory scanning (`listNextLevelDirectories`), comparison logic (`compareDirectories`), and iteration structures.
- **Recommendation**: Consolidate into a unified `DirectoryPipelineProcessor` base class, passing a channel-resolution strategy.

---

## 4. Performance & Concurrency

### 4.1 [Critical] CPU-Burning Busy Loop in ND2 Processing
- **File**: [ND2Images.groovy:L119-L130](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ND2Images.groovy#L119-L130)
- **Problem**:
  ```groovy
  new Thread({
      while (dialog.isVisible()) {
          if (pane.getValue() == "Cancel") {
              timer.cancel()
              dialog.dispose()
              cancelSelected.set(true)
              latch.countDown()
              break
          }
      }
  }).start()
  ```
  This `while(dialog.isVisible())` thread contains **no sleep, yield, or wait**. For the full 2 seconds that each dialog displays, this thread spins at 100% CPU utilization. Over a large batch (e.g. 50 images), this causes severe CPU throttling and heat.
- **Recommendation**:
  Use a property listener instead of thread polling:
  ```groovy
  pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY) { evt ->
      if (pane.value == "Cancel") {
          timer.cancel()
          dialog.dispose()
          cancelSelected.set(true)
          latch.countDown()
      }
  }
  ```
  Or at minimum add `Thread.sleep(50)` inside the loop.

### 4.2 [Medium] Image Stack Memory Management
- When opening multi-slice TIFF stacks across 3 channels simultaneously (`tif_dic_imp`, `tif_488_imp`, `tif_561_imp`), Fiji's memory consumption spikes.
- While `.close()` is called on each `ImagePlus`, explicit garbage collection or unreferencing (`tif_dic_imp = null`) after batch iterations will mitigate memory fragmentation on large datasets.

---

## 5. Security & Data Protection

- **Arbitrary File Deletion**: In [ProcessDirectory2.groovy:L52-L57](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ProcessDirectory2.groovy#L52-L57) (`deleteUnkownDirectory`), `dir.deleteDir()` recursively deletes non-matching directories in the output tree. If a user inadvertently points the Base Directory to a critical folder (or home directory), data loss could occur.
  - **Mitigation**: Add strict sanity validation confirming that the target directory to delete is strictly a child of `directoryRoot` and contains expected prefix structures before issuing recursive deletions.

---

## 6. Actionable Prioritized Checklist

- [ ] **Fix 1 (Immediate)**: Update [ProcessDirectorySpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/process/ProcessDirectorySpec.groovy) and remove [CalculatorSpec.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/test/groovy/com/amywalkerlab/rotato_roi/CalculatorSpec.groovy) so `./gradlew test` passes cleanly.
- [ ] **Fix 2 (Immediate)**: Replace the CPU spinning loop in [ND2Images.groovy:L119](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/ND2Images.groovy#L119) with a `PropertyChangeListener` or `Thread.sleep(50)`.
- [ ] **Fix 3 (Immediate)**: Replace hardcoded `/` path replacements in [RotateImages2.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/RotateImages2.groovy) and [CropImages.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/process/CropImages.groovy) with `File.separator`.
- [ ] **Fix 4 (Maintainability)**: Standardize class naming between [RotatoROI_II.groovy](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/groovy/com/amywalkerlab/rotato_roi_ii/RotatoROI_II.groovy) and [plugins.config](file:///Users/dan/Code/groovy/rotatoROI_plugin_II/app/src/main/resources/plugins.config).
- [ ] **Fix 5 (Architecture)**: Consolidate `ProcessDirectory2` and `ProcessChannelSplitDirectory` into a unified hierarchy with configurable channel definitions.
