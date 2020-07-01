// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: http://www.viva64.com

#include <iostream>
#include <filesystem>
#include <atlbase.h>
#include "CascLib.h"

static bool ExtractFile(HANDLE hStorage, const CASC_FIND_DATA& findData, FILE* outFile, ULONGLONG& totalWritten);
static int  CleanUp(int exitCode, HANDLE hStorage, HANDLE hFind) noexcept;
static bool CleanUpStorage(HANDLE hStorage) noexcept;
static bool CleanUpFind(HANDLE hFind) noexcept;
static FILE* OpenExtractedFile(const char* filePath);

/// <summary>
/// 
/// </summary>
/// <param name="argc"></param>
/// <param name="argv"></param>
/// <returns></returns>
int main(const int argc, const char* argv[])
{
	try {
		LPCTSTR path = nullptr;
		LPCSTR mask = nullptr;
		LPCSTR targetPath = nullptr;
#ifndef _DEBUG
		if (argc != 4) {
			std::cout << "ERROR: Please provide 3 arguments: CascExtractor.exe [\"Path\\to\\the\\CASC storage\"] [\"*mask.txt\"] [\"Path\\to\\the\\output Directory\"]." << std::endl;
			return EXIT_FAILURE;
		}

		std::string str = argv[1];
		std::replace(str.begin(), str.end(), '/', '\\');
		path = CA2CT(str.c_str());

		mask = argv[2];

		str = argv[3];
		std::replace(str.begin(), str.end(), '/', '\\');
		targetPath = str.c_str();

#else
		path = L"C:\\Spiele\\StarCraft II\\";
		mask = "*.SC2Layout";
		targetPath = "Debug\\out";
#endif
		std::wcout << "casc path: " << path << std::endl;
		std::cout << "mask: " << mask << std::endl;
		std::cout << "target path: " << targetPath << std::endl;

		std::wcout << "Opening CASC Storage: " << path << std::endl;

		HANDLE hStorage;

		if (!CascOpenStorage(path, 0, &hStorage)) {
			std::cerr << "ERROR: opening storage: " << GetLastError() << std::endl;
			return EXIT_FAILURE;
		}

		std::cout << "Finding files with mask: " << mask << std::endl;

		PCASC_FIND_DATA pFindData = &CASC_FIND_DATA();
		LPCTSTR listFile = NULL;
		HANDLE hFind = CascFindFirstFile(hStorage, mask, pFindData, listFile);
		if (hFind == INVALID_HANDLE_VALUE) {
			if (GetLastError() == ERROR_SUCCESS) {
				std::cout << "Found 0 files for mask: " << mask << std::endl;
				CascFindClose(hFind);
				CleanUpStorage(hStorage);
				return EXIT_SUCCESS;
			}
			else {
				std::cerr << "ERROR: Unknown error, mask: " << mask << std::endl;
				CascFindClose(hFind);
				CleanUpStorage(hStorage);
				return EXIT_FAILURE;
			}
		}

		namespace fs = std::filesystem;

		int results = 0;
		FILE* pOutFile = nullptr;
		do {
			std::cout << pFindData->szFileName << std::endl;
			std::string targetFilePath = std::string(targetPath).append("\\").append(pFindData->szFileName);

			// get the path from file
			std::string targetDirPath;
			const std::size_t pos = targetFilePath.find_last_of('\\');
			if (pos != std::string::npos) {
				targetDirPath = targetFilePath.substr(0, pos);
				//std::cout << "creating directory: " << targetDirPath.c_str() << std::endl;

				fs::create_directories(targetDirPath.c_str());

				// create filestream
				pOutFile = OpenExtractedFile(targetFilePath.c_str());
				if (pOutFile) {
					ULONGLONG totalWritten = 0;
					ExtractFile(hStorage, *pFindData, pOutFile, totalWritten);
					fclose(pOutFile);

					// remove empty file
					if (totalWritten == 0) {
						remove(targetFilePath.c_str());
						//std::cout << "removed empty file: " << targetFilePath.c_str() << std::endl;
					}
					else {
						++results;
					}
				}
			}
			else {
				std::cerr << "   ERROR: Unexpected file path: " << targetFilePath << std::endl;
			}
		} while (CascFindNextFile(hFind, pFindData));
		pOutFile = nullptr;

		// remove empty directories recursively
		fs::path top = fs::path(targetPath);
		std::vector<fs::path> directories;
		for (auto& p : fs::recursive_directory_iterator(top)) {
			if (fs::is_directory(p)) {
				directories.emplace_back(fs::canonical(p));
			}
		}
		const auto itend = directories.rend();
		for (std::vector<fs::path>::reverse_iterator rit = directories.rbegin(); rit != itend; ++rit) {
			if (fs::is_empty(*rit)) {
				fs::remove(*rit);
			}
		}

		std::cout << "Extracted " << results << " file" << (results == 1 ? "" : "s") << " for mask: " << mask << std::endl;

		// clean up
		const bool findFailed = (GetLastError() != ERROR_NO_MORE_FILES && GetLastError() != ERROR_SUCCESS);
		const int exitCode = findFailed ? EXIT_FAILURE : EXIT_SUCCESS;
		return CleanUp(exitCode, hStorage, hFind);
	}
	catch (std::exception e) {
		std::cout << "ERROR: " << e.what() << std::endl;
	}
	catch (...) {
		std::cout << "An unknown fatal error occurred." << std::endl;
	}
}

/// <summary>
/// Cleans up the Storage and the Find. Returns the specified EXIT_code.
/// </summary>
/// <param name="exitCode">EXIT_code when cleaning up was successful</param>
/// <param name="hStorage">HANDLE of the Storage</param>
/// <param name="hFind">HANDLE of the Find</param>
/// <returns>EXIT_FAILURE code when cleaning up encountered a problem. Else the specified exitCode is returned.</returns>
static int CleanUp(int exitCode, HANDLE hStorage, HANDLE hFind) noexcept {
	bool result = CleanUpFind(hFind);
	result = CleanUpStorage(hStorage) || result;
	return !result ? EXIT_FAILURE : exitCode;
}

/// <summary>
/// 
/// </summary>
/// <param name="hStorage">HANDLE of the Storage</param>
static bool CleanUpStorage(HANDLE hStorage) noexcept {
	if (hStorage) {
		return CascCloseStorage(hStorage);
	}
	return true;
}

/// <summary>
/// 
/// </summary>
/// <param name="hFind">HANDLE of the Find</param>
static bool CleanUpFind(HANDLE hFind) noexcept {
	if (hFind) {
		return CascFindClose(hFind);
	}
	return true;
}





// -----------------------------------------------
// BASED ON THE CODE FROM CASCLIB's TEST PROJECT
// -----------------------------------------------

template <typename T> T* CASC_ALLOC(size_t nCount) noexcept
{
	return (T*)malloc(nCount * sizeof(T));
}

template <typename T> void CASC_FREE(T*& ptr) noexcept
{
	free(ptr);
	ptr = nullptr;
}

// Retrieves the pointer to plain name
template <typename XCHAR> const XCHAR* GetPlainFileName(const XCHAR* szFileName)
{
	const XCHAR* szPlainName = szFileName;

	while (*szFileName != 0)
	{
		if (*szFileName == '\\' || *szFileName == '/')
			szPlainName = szFileName + 1;
		szFileName++;
	}

	return szPlainName;
}

static PCASC_FILE_SPAN_INFO GetFileInfo(HANDLE hFile, CASC_FILE_FULL_INFO& FileInfo) noexcept
{
	PCASC_FILE_SPAN_INFO pSpans = nullptr;
	// Retrieve the full file info
	if (CascGetFileInfo(hFile, CascFileFullInfo, &FileInfo, sizeof(CASC_FILE_FULL_INFO), nullptr))
	{
		if ((pSpans = CASC_ALLOC<CASC_FILE_SPAN_INFO>(FileInfo.SpanCount)) != nullptr)
		{
			if (!CascGetFileInfo(hFile, CascFileSpanInfo, pSpans, FileInfo.SpanCount * sizeof(CASC_FILE_SPAN_INFO), nullptr))
			{
				CASC_FREE(pSpans);
				pSpans = nullptr;
			}
		}
	}
	return pSpans;
}

// removed unnecessary steps/counters
static bool ExtractFile(HANDLE hStorage, const CASC_FIND_DATA& findData, FILE* outFile, ULONGLONG& totalWritten) {
	PCASC_FILE_SPAN_INFO pSpans = nullptr;
	CASC_FILE_FULL_INFO fileInfo{};
	LPCSTR szOpenName = findData.szFileName;
	DWORD dwErrCode = ERROR_SUCCESS;
	HANDLE hFile = NULL;
	totalWritten = 0;

	if (CascOpenFile(hStorage, szOpenName, NULL, CASC_STRICT_DATA_CHECK, &hFile))
	{
		// Retrieve the information about file spans.
		if ((pSpans = GetFileInfo(hFile, fileInfo)) != NULL)
		{
			DWORD dwBytesRead = 0;

			// Load the entire file, one read request per span.
			// Using span-aligned reads will cause CascReadFile not to do any caching,
			// and the amount of memcpys will be almost zero
			for (DWORD i = 0; i < fileInfo.SpanCount && dwErrCode == ERROR_SUCCESS; i++)
			{
				const PCASC_FILE_SPAN_INFO pFileSpan = pSpans + i;
				LPBYTE pbFileSpan = nullptr;
				DWORD cbFileSpan = (DWORD)(pFileSpan->EndOffset - pFileSpan->StartOffset);

				// Do not read empty spans.
				if (cbFileSpan == 0)
					continue;

				// Allocate span buffer
				pbFileSpan = CASC_ALLOC<BYTE>(cbFileSpan);
				if (pbFileSpan == NULL)
				{
					std::cerr << "   ERROR: Not enough memory to allocate " << cbFileSpan << " bytes" << std::endl;
					dwErrCode = ERROR_NOT_ENOUGH_MEMORY;
					break;
				}

				// CascReadFile will read as much as possible. If there is a frame error
				// (e.g. MD5 mismatch, missing encryption key or disc error),
				// CascReadFile only returns frames that are loaded entirely.
				if (!CascReadFile(hFile, pbFileSpan, cbFileSpan, &dwBytesRead))
				{
					// Do not report some errors; for example, when the file is encrypted,
					// we can't do much about it. Only report it if we are going to extract one file
					switch (dwErrCode = GetLastError())
					{
					case ERROR_SUCCESS: {
					} break;
					case ERROR_FILE_ENCRYPTED: {
						std::cout << "   WARNING: File is encrypted: " << szOpenName << std::endl;
					} break;
					default: {
						std::cout << "   WARNING: Could not read file: " << szOpenName << std::endl;
					}break;
					}
				}

				// If required, write the file data to the output file
				if (outFile)
					fwrite(pbFileSpan, 1, dwBytesRead, outFile);

				totalWritten += dwBytesRead;

				// Free the memory occupied by the file span
				CASC_FREE(pbFileSpan);
			}

			// Check whether the total size matches
			if (dwErrCode == ERROR_SUCCESS && totalWritten != fileInfo.ContentSize)
			{
				std::cout << "   WARNING: exported file is corrupted: " << szOpenName << std::endl;
				dwErrCode = ERROR_FILE_CORRUPT;
			}

			// Free the span array
			CASC_FREE(pSpans);
		}

		// Close the handle
		CascCloseFile(hFile);
	}
	else
	{
		std::cout << "   WARNING: could not open file: " << szOpenName << std::endl;
		dwErrCode = GetLastError();
	}

	return dwErrCode;
}

// this is pretty much a new function
static FILE* OpenExtractedFile(const char* filePath)
{
	FILE* pFile;
	errno_t err;
	//std::cout << "outFile: " << filePath << std::endl;
	if ((err = fopen_s(&pFile, filePath, "wb")) != 0) {
		char buf[1024];
		strerror_s(buf, sizeof buf, err);
		std::cout << "ERROR: could not write file: " << filePath << ". Reason: " << buf << std::endl;
		return nullptr;
	}
	return pFile;
}