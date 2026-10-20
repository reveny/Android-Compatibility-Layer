#include <cstdio>

#include "Launcher/Launcher.hpp"

int main(int argc, char** argv)
{
    Launcher::RememberArguments(argc, argv);

    Launcher::Options options;
    Launcher::Result result = Launcher::ParseArguments(argc, argv, options);
    if (result != Launcher::Result::Ok)
    {
        fprintf(stderr, "[acl] %s\n", Launcher::ResultAdvice(result));

        if (result == Launcher::Result::UsageError) Launcher::PrintUsage(argv[0]);

        return 2;
    }

    int exitStatus = 1;

    if (options.action != Launcher::Options::Action::Run)
    {
        result = Launcher::ManagePackages(options, exitStatus);

        if (result != Launcher::Result::Ok)
        {
            fprintf(stderr, "[acl] %s\n", Launcher::ResultAdvice(result));
            return 2;
        }

        return exitStatus;
    }

    result = Launcher::Run(options, exitStatus);
    if (result != Launcher::Result::Ok)
    {
        fprintf(stderr, "[acl] %s\n", Launcher::ResultAdvice(result));

        if (!options.verbose) fprintf(stderr, "[acl] run again with --verbose to see what the runtime was doing\n");

        return exitStatus == 0 ? 1 : exitStatus;
    }

    return exitStatus;
}
