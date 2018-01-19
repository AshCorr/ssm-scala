package com.gu.ssm

import java.io.File

import com.amazonaws.regions.{Region, Regions}
import scopt.OptionParser

trait ArgumentParser {

  val argParser = new OptionParser[Arguments]("ssm") {
    opt[String]("profile").required()
      .action { (profile, args) =>
        args.copy(profile = Some(profile))
      } text "the AWS profile name to use for authenticating this execution"
    opt[String]("region").optional()
      .validate { region =>
        try {
          Region.getRegion(Regions.fromName(region))
          success
        } catch {
          case e: IllegalArgumentException =>
            failure(s"Invalid AWS region name, $region")
        }
      } action { (region, args) =>
      args.copy(region = Region.getRegion(Regions.fromName(region)))
    } text "AWS region name (defaults to eu-west-1)"
    // TODO: make these args instead of opts
    opt[Seq[String]]('i', "instances")
      .action { (instanceIds, args) =>
        val instances = instanceIds.map(i => InstanceId(i)).toList
        args.copy(executionTarget = Some(ExecutionTarget(instances = Some(instances))))
      } text "specify the instance ID(s) on which the specified command(s) should execute"
    opt[String]('t', "ass-tags")
      .validate { tagsStr =>
        Logic.extractSASTags(tagsStr).map(_ => ())
      }
      .action { (tagsStr, args) =>
        Logic.extractSASTags(tagsStr)
          .fold(
            _ => args,
            ass => args.copy(executionTarget = Some(ExecutionTarget(ass = Some(ass))))
          )
      } text "search for instances by tag e.g. --ssa-tags app,stack,stage"
    opt[String]('c', "cmd")
      .action { (command, args) =>
        args.copy(toExecute = Some(ToExecute(cmdOpt = Some(command))))
      } text "a (bash) command to execute"
    opt[File]('f', "src-file")
      .action { (file, args) =>
        args.copy(toExecute = Some(ToExecute(scriptOpt = Some(file))))
      } text "a file containing bash commands to execute"
    opt[Unit]('I', "interactive")
      .action { (_, args) =>
        args.copy(interactive = true)
      } text "run SSM in interactive mode"
    opt[Unit]('s', "ssh")
      .action { (_, args) =>
        args.copy(ssh = true)
      } text "run SSM in interactive mode"
    checkConfig { args =>
      if (args.toExecute.isEmpty && !args.interactive && !args.ssh) {
        Left("You must provide cmd or src-file; or interactive; or ssh")
      } else {
        if (args.executionTarget.isEmpty) {
          Left("You must provide a list of target instances, Stack, Stage, App tags")
        } else {
          Right(())
        }
      }
    }
  }
}
