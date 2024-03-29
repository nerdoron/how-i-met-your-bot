package me.nerdoron.himyb.commands.currency;

import java.sql.SQLException;
import java.util.Map;

import me.nerdoron.himyb.Global;
import me.nerdoron.himyb.commands.SlashCommand;
import me.nerdoron.himyb.modules.brocoins.BroCoinsSQL;
import me.nerdoron.himyb.modules.brocoins.Sorter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BankCommand extends SlashCommand {

    final Logger logger = LoggerFactory.getLogger(Global.className(this.getClass()));
    BroCoinsSQL broCoinsSQL = new BroCoinsSQL();

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        String subcmd = event.getSubcommandName();
        if (subcmd.equals("create")) {
            if (broCoinsSQL.hasBrocoins(member) == true) {
                event.reply("You already have a bank account!").setEphemeral(true).queue();
                return;
            }
            try {
                broCoinsSQL.setBrocoins(member, 5);
                event.reply("Created a BroBank account! I gave you 5 " + Emoji.fromCustom(Global.broCoin).getAsMention()
                        + " to get started!").setEphemeral(true)
                        .queue();
                logger.info(member.getUser().getAsTag() + "(" + member.getId() + ") Created a bank account");
            } catch (SQLException e) {
                event.reply("Error!").queue();
                e.printStackTrace();
            }
        }

        // check
        if (subcmd.equals("check")) {
            if (broCoinsSQL.hasBrocoins(member) == false) {
                event.reply("You dont have a bank account!").setEphemeral(true).queue();
                return;
            }
            if (event.getInteraction().getOptions().isEmpty()) {
                int memberCoins = broCoinsSQL.getBrocoins(member);
                event.reply("You have " + memberCoins + " " + Emoji.fromCustom(Global.broCoin).getAsMention() + ".")
                        .setEphemeral(true).queue();
                return;
            }
            Member memberToCheck = event.getInteraction().getOption("user").getAsMember();
            int memberCoins = broCoinsSQL.getBrocoins(memberToCheck);
            if (broCoinsSQL.hasBrocoins(memberToCheck) == false) {
                event.reply(memberToCheck.getEffectiveName() + " doesn't have a bank account!").setEphemeral(true)
                        .queue();
                return;
            }

            MessageBuilder msb = new MessageBuilder();
            msb.setContent(memberToCheck.getEffectiveName() + " has " + memberCoins + " "
                    + Emoji.fromCustom(Global.broCoin).getAsMention() + ".");
            msb.denyMentions(Message.MentionType.values());

            event.reply(msb.build()).queue();
        }

        // transfer
        if (subcmd.equals("transfer")) {
            if (!broCoinsSQL.hasBrocoins(member)) {
                event.reply("You dont have a bank account!").setEphemeral(true).queue();
                return;
            }
            int userCoins = broCoinsSQL.getBrocoins(member);
            int amountToTransfer = event.getInteraction().getOption("amount").getAsInt();

            if (userCoins < amountToTransfer) {
                event.reply("You dont have enough BroCoins to transfer!").setEphemeral(true).queue();
                return;
            }

            Member memberToTransferTo = event.getInteraction().getOption("user").getAsMember();
            if (!broCoinsSQL.hasBrocoins(memberToTransferTo)) {
                event.reply("That user does not have a BroBank account. Please tell them to create one.")
                        .setEphemeral(true).queue();
                return;
            }
            try {
                broCoinsSQL.updateBrocoins(memberToTransferTo, amountToTransfer);
                broCoinsSQL.updateBrocoins(member, -amountToTransfer);
                event.reply("Transferred " + amountToTransfer + " " + Emoji.fromCustom(Global.broCoin).getAsMention()
                        + " to "
                        + memberToTransferTo.getAsMention()).queue();

                int memberBrocoins = broCoinsSQL.getBrocoins(member);
                int targetBrocoins = broCoinsSQL.getBrocoins(memberToTransferTo);
                logger.info(member.getUser().getAsTag() + "(" + member.getId() + ") Transfered (" + amountToTransfer
                        + " Coins) now they have (" + memberBrocoins + " Coins) and "
                        + memberToTransferTo.getUser().getAsTag() + "(" + memberToTransferTo.getId() + ") has ("
                        + targetBrocoins + " Coins)");
            } catch (SQLException e) {
                event.reply("Error!").queue();
                e.printStackTrace();
            }
        }

        // leaderboard
        if (subcmd.equals("leaderboard")) {
            Map<String, Integer> brocoins = broCoinsSQL.getBrocoins();
            Map<String, Integer> sorted = new Sorter().sortMapMaxLow(brocoins, 10);
            int longest = 0;
            for (Integer p : sorted.values()) {

                String a = p + "";
                if (a.length() > longest) {
                    longest = a.length();
                }
            }

            String users = "";
            for (String userID : sorted.keySet()) {
                int coins = sorted.get(userID);
                if (coins > 0) {
                    String textCoins = coins + "";
                    for (int i = textCoins.length(); i < longest; i++) {
                        textCoins = " " + textCoins;
                    }
                    users += "**`" + textCoins + "`** " + Global.broCoin.getAsMention() + " | " + "<@!" + userID + ">"
                            + "\n";
                }
            }

            EmbedBuilder emb = new EmbedBuilder();
            emb.setColor(Global.embedColor);
            emb.setTitle("Brocoins Leaderboard");
            emb.setDescription(users);

            event.replyEmbeds(emb.build()).setEphemeral(false).queue();

        }
    }

    @Override
    public SlashCommandData getSlash() {
        SlashCommandData bank = Commands.slash("bank", "Control your BroBank account");
        SubcommandData b_create = new SubcommandData("create", "Create a BroBank account");
        SubcommandData b_transfer = new SubcommandData("transfer",
                "Transfers a set amount of coins from your bank account to another person's bank account");
        b_transfer.addOption(OptionType.USER, "user", "The user you want to transfer to.", true);
        OptionData transferAmount = new OptionData(OptionType.INTEGER, "amount",
                "The amount of coins you want to transfer", true);
        transferAmount.setMinValue(1);
        b_transfer.addOptions(transferAmount);

        SubcommandData b_check = new SubcommandData("check", "Checks how many BroCoins you have");
        b_check.addOption(OptionType.USER, "user", "The user you want to check.", false);

        SubcommandData b_leaderboard = new SubcommandData("leaderboard",
                "Shows the top 10 richest people in the server");

        bank.addSubcommands(b_create, b_check, b_transfer, b_leaderboard);
        return bank;
    }

}
