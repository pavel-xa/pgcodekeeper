CREATE TYPE [dbo].[MyType] FROM [tinyint]
GO

CREATE SEQUENCE [dbo].[myseq]
    AS [dbo].[MyType]
    INCREMENT BY 1
    MAXVALUE 255
    MINVALUE 0
GO
