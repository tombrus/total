#!/bin/bash

export JAVA_HOME="$(/usr/libexec/java_home -v 16)"

    MAIN_VERSION="1.0.0"
       MAIN_NAME="total"
       MAIN_ICON="graph-512.icns"
      MAIN_CLASS="nl.tombrus.total.Main"
 MAIN_CLASS_FILE="out/production/total/${MAIN_CLASS//.//}.class"
        MAIN_JAR="total.jar"
        LIB_JARS=("lib/mvg-json-1.6.3.jar")

    JAVA_VERSION="15"
        JARS_DIR="out/artifacts/total"
JAVAFX_JMODS_DIR="lib/javafx-jmods-15.0.1"
 JAVAFX_JARS_DIR="lib/javafx-sdk-15.0.1/lib"
   EXTRA_MODULES="jdk.crypto.ec,jdk.localedata,javafx.controls,javafx.fxml"

     TMP_RUNTIME="out/java-runtime"
   TMP_INSTALLER="out/installer"
        TMP_LIBS="$TMP_INSTALLER/input/libs"

echo "########## prepare..."
rm -rfd "$TMP_RUNTIME" "$TMP_INSTALLER"
mkdir -p "$TMP_LIBS"
cp "$JARS_DIR/$MAIN_JAR" "$JAVAFX_JARS_DIR"/*.jar "${LIB_JARS[@]}" "$TMP_LIBS"

echo "########## detecting required modules..."
detected_modules="$(
    "$JAVA_HOME/bin/jdeps" \
        -q \
        --multi-release             "$JAVA_VERSION" \
        --module-path               "$JAVAFX_JMODS_DIR" \
        --ignore-missing-deps \
        --print-module-deps \
        --class-path                "$TMP_LIBS"/*.jar "$MAIN_CLASS_FILE"
)"
echo "           detected modules:"
echo "$detected_modules" | tr ',' '\n' | sort | sed 's/^/               /'

echo "########## creating java runtime image..."
"$JAVA_HOME/bin/jlink" \
        --strip-native-commands \
        --no-header-files \
        --no-man-pages  \
        --compress="2"  \
        --strip-debug \
        --module-path               "$JAVAFX_JMODS_DIR" \
        --add-modules               "$EXTRA_MODULES,$detected_modules" \
        --include-locales="en,nl" \
        --output                    "$TMP_RUNTIME"

echo "########## creating installer..."
"$JAVA_HOME/bin/jpackage" \
        --type                      "app-image" \
        --dest                      "$TMP_INSTALLER" \
        --input                     "$TMP_LIBS" \
        --name                      "$MAIN_NAME" \
        --main-class                "$MAIN_CLASS" \
        --main-jar                  "$MAIN_JAR" \
        --java-options              "-Xmx2048m" \
        --runtime-image             "$TMP_RUNTIME" \
        --icon                      "$MAIN_ICON" \
        --app-version               "$MAIN_VERSION" \
        --vendor                    "Tom Brus" \
        --copyright                 "Copyright ©2021 Tom Brus" \
        --mac-package-identifier    "nl.tombrus.$MAIN_NAME.app" \
        --mac-package-name          "$MAIN_NAME"
echo "           generated app: $PWD/$TMP_INSTALLER/$MAIN_NAME.app"

echo "########## install on desktop..."
rm -rf ~/"Desktop/$MAIN_NAME.app"
cp -r "$TMP_INSTALLER/$MAIN_NAME.app" ~/"Desktop"

echo "########## done"

#open --stdout "/tmp/ooo" --stderr "/tmp/eee" -a "$PWD/$TMP_INSTALLER/$MAIN_NAME.app"
#tail -f /tmp/{ooo,eee}