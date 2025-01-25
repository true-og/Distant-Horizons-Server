import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

class RelocateNatives
{
	private static boolean prepared = false;
	
	private static final Path rootDirectory = Path.of(System.getProperty("user.dir"), "relocate_natives");
	private static final Path cacheRoot = rootDirectory.resolve("cache");
	
	
	@SuppressWarnings({"ResultOfMethodCallIgnored", "BusyWait"})
	private static CompletableFuture<Void> readOutputStreams(Process process)
	{
		return CompletableFuture.runAsync(() -> {
			try
			{
				while (process.isAlive() || process.getInputStream().available() > 0 || process.getErrorStream().available() > 0)
				{
					if (process.getInputStream().available() > 0)
					{
						byte[] data = new byte[process.getInputStream().available()];
						process.getInputStream().read(data);
						System.out.write(data);
					}
					if (process.getErrorStream().available() > 0)
					{
						byte[] data = new byte[process.getErrorStream().available()];
						process.getErrorStream().read(data);
						System.err.write(data);
					}
					Thread.sleep(100);
				}
			}
			catch (Exception ignored)
			{
			}
		});
	}
	
	private static void ensurePrepared() throws Exception
	{
		if (prepared)
		{
			return;
		}
		prepared = true;
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(rootDirectory.toFile());
		
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
		{
			processBuilder.command("powershell", "./prepare.ps1");
		}
		else if (os.contains("nix") || os.contains("nux") || os.contains("mac"))
		{
			processBuilder.command("./prepare.sh");
		}
		else
		{
			throw new IllegalStateException("Unsupported operating system: " + os);
		}
		
		Process process = processBuilder.start();
		CompletableFuture<Void> outputFuture = readOutputStreams(process);
		
		int exitCode = process.waitFor();
		outputFuture.get();
		
		if (exitCode != 0)
		{
			throw new Exception("Prepare failed: " + exitCode);
		}
	}
	
	public static byte[] updateUsingLief(Path outputFilePath, byte[] content) throws Exception
	{
		ensurePrepared();
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(rootDirectory.toFile());
		
		processBuilder.command(
				rootDirectory.resolve(".venv/Scripts").toFile().exists()
						? rootDirectory.resolve(".venv/Scripts/python.exe").toString()
						: rootDirectory.resolve(".venv/bin/python").toString(),
				"./process.py",
				outputFilePath.toString()
		);
		
		Process process = processBuilder.start();
		CompletableFuture<Void> outputFuture = readOutputStreams(process);
		
		process.getOutputStream().write(content);
		process.getOutputStream().close();
		
		int exitCode = process.waitFor();
		outputFuture.get();
		
		if (exitCode != 0)
		{
			throw new Exception("Process failed: " + exitCode);
		}
		
		return Files.readAllBytes(outputFilePath);
	}
	
	private static void replaceInByteArray(byte[] byteArray, String target, String replacement)
	{
		if (target.length() < replacement.length())
		{
			throw new IllegalArgumentException("Replacement must be the same length or shorter than the target.");
		}
		
		byte[] targetBytes = target.getBytes(StandardCharsets.US_ASCII);
		byte[] replacementBytes = replacement.getBytes(StandardCharsets.US_ASCII);
		
		byte nullByte = 0;
		
		for (int endPos = 0; endPos < byteArray.length - targetBytes.length - 1; endPos++)
		{
			int startPos = endPos;
			int targetPos = 0;
			while (targetPos < targetBytes.length && byteArray[endPos] == targetBytes[targetPos])
			{
				targetPos++;
				endPos++;
			}
			
			if (targetPos == targetBytes.length)
			{
				System.arraycopy(replacementBytes, 0, byteArray, startPos, replacementBytes.length);
				
				startPos = startPos + replacementBytes.length;
				while (byteArray[endPos] != nullByte)
				{
					byteArray[startPos] = byteArray[endPos];
					endPos++;
					startPos++;
				}
				byteArray[startPos] = nullByte;
			}
		}
	}
	
	public static byte[] processNative(String path, byte[] content) throws Exception
	{
		Path outputFilePath = cacheRoot.resolve(path);
		outputFilePath.getParent().toFile().mkdirs();
		
		if (outputFilePath.toFile().exists())
		{
			return Files.readAllBytes(outputFilePath);
		}
		
		replaceInByteArray(content, "org_sqlite", "dh_1sqlite");
		replaceInByteArray(content, "org/sqlite", "dh_sqlite");
		return updateUsingLief(outputFilePath, content);
	}
	
}
