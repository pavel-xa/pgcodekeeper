ALTER TABLE [dbo].[table1]
	ADD CONSTRAINT [PK_table1] PRIMARY KEY CLUSTERED  ([c1]) ON [PRIMARY]
GO

ALTER TABLE [dbo].[table1]
	ADD CONSTRAINT [constraint_check_c2] CHECK (c2 > 10)
GO

ALTER TABLE [dbo].[table1]
	ADD CONSTRAINT [constraint_default_c2] DEFAULT 50 FOR c2
GO