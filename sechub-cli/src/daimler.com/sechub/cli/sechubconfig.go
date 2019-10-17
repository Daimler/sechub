// SPDX-License-Identifier: MIT
package cli

import (
	"bytes"
	"encoding/json"
	"fmt"
	"html/template"
	"io/ioutil"
	"os"
	"strings"
)

// The configuration pendant in Go. But we do only necessary parts from JSON file!
// so Webscan, InfraScan are not handled here (but still uploaded)
// Only code scan is necessary, because determination necessary if there is an upload necessary or not.
type SecHubConfig struct {
	APIVersion string         `json:"apiVersion"`
	User       string         `json:"user"`
	ProjectId  string         `json:"project"`
	Server     string         `json:"server"`
	CodeScan   CodeScanConfig `json:"codeScan"`
}

// CodeScanConfig contains information how code scan shall be done
type CodeScanConfig struct {
	FileSystem FileSystemConfig `json:"fileSystem"`
	Excludes   []string         `json:"excludes"`
}

// FileSystemConfig contains data for folders
type FileSystemConfig struct {
	Folders []string `json:"folders"`
}

func fillTemplate(templateSource string, data map[string]string) []byte {
	var tpl bytes.Buffer
	t := template.Must(template.New("sechubConfig").Parse(templateSource))

	err := t.Execute(&tpl, data)

	if err != nil {
		fmt.Println("sechub confiuration json is is not a valid template")
		showHelpHint()
		os.Exit(EXIT_CODE_MISSING_CONFIGFILE)
	}
	return tpl.Bytes()
}

func newSecHubConfigFromBytes(bytes []byte) SecHubConfig {
	var sechubConfig SecHubConfig

	/* transform text to json */
	err := json.Unmarshal(bytes, &sechubConfig)
	if err != nil {
		fmt.Println("sechub confiuration json is not valid json")
		showHelpHint()
		os.Exit(EXIT_CODE_MISSING_CONFIGFILE)
	}
	return sechubConfig
}

func showHelpHint() {
	fmt.Println("Call sechub with --help option to show correct usage and examples")
}

func newSecHubConfigurationFromFile(context *Context, filePath string) SecHubConfig {
	LogDebug(context, fmt.Sprintf("Loading config file: '%s'\n", filePath))

	/* open file and check exists */
	jsonFile, err := os.Open(filePath)
	defer jsonFile.Close()

	if err != nil {
		fmt.Println(err)
		showHelpHint()
		os.Exit(EXIT_CODE_MISSING_CONFIGFILE)
	}

	/* read text content as "unfilled byte value". This will be used for debug outputs,
	   so we do not have passwords etc. accidently leaked */
	context.unfilledByteValue, err = ioutil.ReadAll(jsonFile)
	if err != nil {
		fmt.Println(err)
		showHelpHint()
		os.Exit(EXIT_CODE_MISSING_CONFIGFILE)
	}

	data, _ := envToMap()
	context.byteValue = fillTemplate(string(context.unfilledByteValue), data)

	return newSecHubConfigFromBytes(context.byteValue)
}

func envToMap() (map[string]string, error) {
	envMap := make(map[string]string)
	var err error

	for _, v := range os.Environ() {
		splitted := strings.Split(v, "=")
		envMap[splitted[0]] = splitted[1]
	}

	return envMap, err
}
