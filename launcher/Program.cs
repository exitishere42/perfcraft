using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

namespace PerfCraftLauncher
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                string appDir = AppDomain.CurrentDomain.BaseDirectory;
                
                // Find perfcraft-*-all.jar in the application directory
                string[] jars = Directory.GetFiles(appDir, "perfcraft-*-all.jar");
                string targetJar = null;
                if (jars.Length > 0)
                {
                    Array.Sort(jars);
                    targetJar = jars[jars.Length - 1]; // pick newest
                }
                else
                {
                    // Fallback to checking target/ or local folder
                    string targetDir = Path.Combine(appDir, "target");
                    if (Directory.Exists(targetDir))
                    {
                        string[] targetJars = Directory.GetFiles(targetDir, "perfcraft-*-all.jar");
                        if (targetJars.Length > 0)
                        {
                            Array.Sort(targetJars);
                            targetJar = targetJars[targetJars.Length - 1];
                        }
                    }
                }

                if (string.IsNullOrEmpty(targetJar) || !File.Exists(targetJar))
                {
                    MessageBox.Show(
                        "PerfCraft JAR-Datei konnte nicht gefunden werden!\nBitte führen Sie install.bat aus.",
                        "PerfCraft",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                // Prepare javaw arguments
                // Pass jar and all CLI arguments (such as opened .perf file)
                string javaArgs = "-jar \"" + targetJar + "\"";
                if (args != null && args.Length > 0)
                {
                    for (int i = 0; i < args.Length; i++)
                    {
                        javaArgs += " \"" + args[i] + "\"";
                    }
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = "javaw.exe";
                psi.Arguments = javaArgs;
                psi.WorkingDirectory = appDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;

                try
                {
                    Process.Start(psi);
                }
                catch (Exception)
                {
                    // If javaw is not in PATH, try common JAVA_HOME or Adoptium
                    string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
                    if (!string.IsNullOrEmpty(javaHome))
                    {
                        string javawPath = Path.Combine(javaHome, "bin", "javaw.exe");
                        if (File.Exists(javawPath))
                        {
                            psi.FileName = javawPath;
                            Process.Start(psi);
                            return;
                        }
                    }

                    MessageBox.Show(
                        "Java 21+ (javaw.exe) wurde auf diesem System nicht gefunden.\nBitte installieren Sie Java über adoptium.net oder führen Sie install.bat aus.",
                        "PerfCraft - Java nicht gefunden",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Warning
                    );
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Fehler beim Starten von PerfCraft: " + ex.Message, "PerfCraft Fehler", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
    }
}
